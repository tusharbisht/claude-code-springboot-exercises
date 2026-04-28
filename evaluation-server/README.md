# Evaluation server

A minimal progress webhook receiver in pure Python (no dependencies). Use it for cohort-style learning to track who's passing which exercise.

## Run it

```bash
cd evaluation-server
python3 server.py
# → listening on http://0.0.0.0:5000
```

## Wire learners up

Each learner sets:

```bash
export EVAL_WEBHOOK_URL=http://<your-host>:5000/progress
```

Then either:
- the **Claude Code Stop hook** (in `.claude/settings.json`) POSTs after each session, or
- the **GitHub Actions workflow** POSTs after CI grading (set `EVAL_WEBHOOK_URL` as a repo secret)

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/progress` | accepts a `grading-result.json` payload (validates JSON, appends to log) |
| GET | `/leaderboard` | aggregated submissions per branch |
| GET | `/health` | health check |

## Extending

For a real cohort, replace the file-backed log with a database, add HMAC-signed payloads or per-learner tokens, and put it behind HTTPS. This file is meant to show the contract, not be production-ready.
