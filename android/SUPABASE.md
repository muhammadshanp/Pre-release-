# Supabase

Project: **pre-re** · ref `bjhluvplbozefgmjzefw` · region `ap-south-1`

Only one thing about a person is stored in Supabase: **their display name**. Photos, PDFs,
notes, reels and chat messages stay on the phone and are never uploaded — that promise is
what the app is for, so the database schema is deliberately tiny.

## Schema

```
public.usernames
  username    text primary key        -- normalised: lowercased, trimmed
  device_id   text not null           -- random UUID for the install that claimed it
  created_at  timestamptz not null default now()
```

Row level security is on and the only policy denies everything: the publishable key cannot
`select`, `insert`, `update` or `delete` a row. Everything goes through three
`SECURITY DEFINER` functions, which is what stops one person enumerating everyone else.

| Function | Returns | Notes |
| --- | --- | --- |
| `pre_re_check(p_username)` | `{available, reason}` | `reason`: `ok`, `taken`, `short`, `long`, `chars`, `reserved`, `empty` |
| `pre_re_claim(p_username, p_device)` | `{status}` | `ok`, `already_yours`, `taken`, `reserved`, `bad_device`, … |
| `pre_re_release(p_username, p_device)` | `{released}` | only from the install that holds the name |

Rules enforced server-side: 3–24 chars, `[a-z0-9._]`, must start and end alphanumeric,
and a reserved list (`admin`, `support`, `pre_re`, `pvault`, …). First writer wins; the
same device re-claiming its own name is not an error.

Verified against the live project: `check('admin') → reserved`, `check('helena') → free`,
`claim` by device A → `ok`, `claim` by device B → `taken`, `release` by B → `false`,
`release` by A → `true`.

## From the app

`Supabase.kt` talks PostgREST directly with `HttpURLConnection` — no SDK, no new Gradle
dependency, matching how the rest of the app does HTTP.

```kotlin
Supabase.checkName(context, "helena")   // Free | Taken | Rejected(reason) | Offline
Supabase.claimName(context, "helena")   // "ok" | "taken" | … | null when unreachable
Supabase.releaseName(context, "helena") // Boolean
Supabase.claimProblem(status)           // human wording for any of the above
```

The install id is a UUID in `SharedPreferences` (`pre_re_supabase/device_id`), so a name can
belong to a phone with no account and no email.

## Keys

`sb_publishable_…` ships in the app on purpose; it is inert without RLS. The
`service_role` key is **not** in the repo and must never be — it bypasses every policy.

## Still on Firebase

Google sign-in, the push notifications and the admin panel's address lists remain on
Firebase. Moving names to Supabase did not touch them, and nothing here reads or writes
vault content. A full migration of the admin lists is a separate job.
