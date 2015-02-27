Change Log
==========

## Version 1.0.1

_2015-02-27_

 * **`SharedPreferences` inspection.**
   It's now possible to inspect SharedPreference files from the "Resources"
   tab.

 * New #65: Show non-default process name to chrome://inspect UI.
 * Fix #57: HTTP responses without the Content-Type header do not appear in the
   DevTools UI.
 * Fix #49: Unconditional "Could not bind to socket" error.
 * Fix #37: Duplicate dumpapp endpoints for the same process.
 * Fix: Use raw process name for Stetho sockets to fix an issue formatting
   choices in `dumpapp`

## Version 1.0.0

_2015-02-18_

 * Initial release.
