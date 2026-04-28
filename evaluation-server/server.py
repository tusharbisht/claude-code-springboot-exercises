"""
Reference progress webhook receiver.

Run:   python3 server.py
Then point Claude Code or GitHub Actions at it via:
       export EVAL_WEBHOOK_URL=http://your-host:5000/progress

It accepts POSTed grading-result.json payloads and appends them to
progress.log (one JSON object per line) plus an in-memory leaderboard.

This is intentionally minimal — for a real cohort, replace the file-backed
log with a database, add auth, and put it behind HTTPS.
"""

from __future__ import annotations

import json
import os
import threading
from collections import defaultdict
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, HTTPServer

LOG_PATH = os.environ.get("PROGRESS_LOG", "progress.log")
PORT = int(os.environ.get("PORT", "5000"))

# Aggregate state — branch -> latest known status per source IP.
_lock = threading.Lock()
_state: dict[str, dict[str, dict]] = defaultdict(dict)


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path != "/progress":
            self._respond(404, {"error": "not found"})
            return
        length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(length) if length > 0 else b"{}"
        try:
            payload = json.loads(raw.decode("utf-8"))
        except json.JSONDecodeError as exc:
            self._respond(400, {"error": f"invalid JSON: {exc}"})
            return

        record = {
            "receivedAt": datetime.now(timezone.utc).isoformat(),
            "fromIp": self.client_address[0],
            "payload": payload,
        }
        with _lock:
            with open(LOG_PATH, "a", encoding="utf-8") as f:
                f.write(json.dumps(record) + "\n")
            branch = str(payload.get("branch", "unknown"))
            _state[branch][self.client_address[0]] = payload

        self._respond(200, {"ok": True})

    def do_GET(self):
        if self.path == "/leaderboard":
            with _lock:
                summary = {
                    branch: {
                        "submissions": len(by_ip),
                        "passing": sum(1 for p in by_ip.values()
                                       if p.get("status") == "passed"),
                    }
                    for branch, by_ip in _state.items()
                }
            self._respond(200, summary)
            return
        if self.path == "/health":
            self._respond(200, {"ok": True})
            return
        self._respond(404, {"error": "not found"})

    def _respond(self, status: int, body: dict):
        encoded = json.dumps(body).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def log_message(self, fmt, *args):
        print(f"{self.client_address[0]} - {fmt % args}")


def main():
    server = HTTPServer(("0.0.0.0", PORT), Handler)
    print(f"Listening on http://0.0.0.0:{PORT}")
    print(f"  POST /progress    — accept a grading-result.json payload")
    print(f"  GET  /leaderboard — see aggregated branch progress")
    print(f"  GET  /health      — health check")
    print(f"  log file: {LOG_PATH}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nshutting down")


if __name__ == "__main__":
    main()
