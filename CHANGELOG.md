# Change Log for Tapis Notifications Service

All notable changes to this project will be documented in this file.

Please find documentation here:
https://tapis.readthedocs.io/en/latest/technical/notifications.html

You may also reference live-docs based on the openapi specification here:
https://tapis-project.github.io/live-docs

---------------------------------------------------------------------------
## 0.0.1 - 2022-04-20

Alpha release supporting basic operations:

### Support:
- CRUD for subscriptions with support for: TTL, enable/disable, search, change owner.
- Subscription reaper for removing expired subscriptions
- Publish an event
- Deliver notifications by webhook or email.
- Basic recovery: check for duplicate event on startup, process events in recovery, process interrupted delivery.

---------------------------------------------------------------------------
## 1.0.0 - 2022-??-??

Initial release supporting basic operations. TBD

### Breaking Changes:
- Initial release.

### New features:
 - Initial release.

### Bug fixes:
- None.
