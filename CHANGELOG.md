# Change Log for Tapis Notifications Service

All notable changes to this project will be documented in this file.

Please find documentation here:
https://tapis.readthedocs.io/en/latest/technical/notifications.html

You may also reference live-docs based on the openapi specification here:
https://tapis-project.github.io/live-docs

## 1.6.2 - 2024-??-??

Incremental improvements and new features.

### New features:
- Add sequence counter associated with seriesId attribute of an event. Allows reconstruction of event order for a series.
- Add attribute *received* to Event model. This attribute is the Tapis generated timestamp for when the event was received by Tapis.
- Run service in docker container as non-root user.

### Bug fixes:
- None

---------------------------------------------------------------------------
## 1.6.1 - 2024-03-06

Incremental improvements.

### New features:
- None

### Bug fixes:
- None

---------------------------------------------------------------------------
## 1.6.0 - 2024-01-26

New release

### New features:
- None

### Bug fixes:
- None

---------------------------------------------------------------------------
## 1.5.12 - 2023-12-13

Update handling of anyOwner and ownedBy query parameters.

### New features:
- None

### Bug fixes:
- Fix handling of anyOwner and ownedBy query parameters.

---------------------------------------------------------------------------
## 1.5.11 - 2023-12-08

Bug fix.

### New features:
- None

### Bug fixes:
- A few log message calls were looking in wrong resource bundle for the message.

---------------------------------------------------------------------------
## 1.5.10 - 2023-11-20

Incremental improvements and bug fix.

### New features:
- None

### Bug fixes:
- Rebuild with latest shared code to fix JWT validation issue.

---------------------------------------------------------------------------
## 1.5.0 - 2023-10-11

New release

### New features:
- None

### Bug fixes:
- None

---------------------------------------------------------------------------
## 1.4.1 - 2023-09-22

Code cleanup, update java version in docker builds.

### New features:
- None

### Bug fixes:
- None

---------------------------------------------------------------------------
## 1.4.0 - 2023-07-07

New release

### New features:
- None

### Bug fixes:
- None

---------------------------------------------------------------------------
## 1.3.4 - 2023-05-24

Bug fix.

### New features:
- None

### Bug fixes:
- Fix issue with extracting host from webhook URL.

---------------------------------------------------------------------------
## 1.3.3 - 2023-05-10

Incremental improvements and new features.

### New features:
- Throttle webhook calls by host.
- Validate delivery address for delivery targets.

### Bug fixes:
- None

---------------------------------------------------------------------------
## 1.3.2 - 2023-05-08

Bug fix.

### New features:
- None

### Bug fixes:
- Add package registration of jax-rs filters to ensure query parameters are correctly set.

---------------------------------------------------------------------------
## 1.3.1 - 2023-04-11

Incremental improvements and bug fixes.

### New features:
- Improve readycheck logging.

### Bug fixes:
- Use of TapisExceptionMapper can lead to invalid http return status codes. Use ApiExceptionMapper instead.
- Fix issue with readycheck.

---------------------------------------------------------------------------
## 1.3.0 - 2023-02-24

Initial release supporting basic operations.

### Breaking Changes:
- Initial release.

### New features:
- Initial release.

### Bug fixes:
- Subscription enabled flag not honored.

---------------------------------------------------------------------------
## 0.0.1 - 2022-04-20

Alpha release supporting basic operations.

### Support:
- CRUD for subscriptions with support for: TTL, enable/disable, search, change owner.
- Subscription reaper for removing expired subscriptions
- Publish an event
- Deliver notifications by webhook or email.
- Basic recovery: check for duplicate event on startup, process events in recovery, process interrupted delivery.

