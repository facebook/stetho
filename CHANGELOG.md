Change Log
==========

## Version 1.1.1

_2015-05-01_

 * **Updated patent grant!**
   See https://code.facebook.com/posts/1639473982937255/updating-our-open-source-patent-grant/

 * New: `stetho-timber` added to redirect log messages to the Stetho console.
 * Fix #140: More efficient and simpler Fragment accessor code.
 * Fix #123: All view inspection features are now available for ICS (API 15)
   and up (some features required JB MR2, API 18).
 * Fix #154: Fix subtle race when a database is removed after the DevTools
   UI is opened.
 * Fix #151: Crash when rapidly adding/removing SharedPreferences keys.
 * Fix #142: View inspection "hit testing" didn't work as intended with its
   two-pass design.
 * Fix: Ignore extraneous files when WAL is enabled for SQLite databases.

## Version 1.1.0

_2015-04-02_

 * **View inspection!**
   For ICS (API 15) and up, we now have full View inspection support in the
   "Elements" tab!  Lots of goodies such as `<fragment>` instances virtually
   placed in the hierarchy, view highlighting, and the ability to tap on a view
   to jump to its position in the hierarchy.  Some features are only available
   for JB MR2 (API 18).

 * New #109: Add `dumpapp hprof` plugin to conveniently extract HPROF memory
   dumps.
 * New #110: Add `dumpapp crash` plugin to mechanize process death in a variety
   of ways.
 * New #105: Simplify excluding Stetho from release builds (exercised in
   `stetho-sample`).
 * New #40: Support SQLite databases in arbitrary directories.
 * Fix #115: Support multiple headers with the same name (most notably, HTTP
   cookies).
 * Fix #108: Workaround throughput issue in Android's LocalSocket#flush()
   method.
 * Fix #88: Avoid OOM on huge request/response HTTP payloads.
 * Fix #82: Provide visual feedback for INSERT/UPDATE/DELETE statements.
 * Fix: Javadoc JAR should now be uploaded properly to Maven.

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
