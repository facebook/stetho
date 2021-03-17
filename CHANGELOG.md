Change Log
==========

## Version 1.6.0
_2021-03-17_

 * Fix #662/665: AndroidX upgrade.

## Version 1.5.1
_2019-03-17_

 * **Fix view inspection crash on Android P+**
   View highlighting was using a legacy pre-O API which was removed in P that caused
   a crash when highlighting a view in the Elements tab.  This has now been fixed (#607)
   and the Elements tab is expected to work normally again.

 * Fix #325/295: Add ability to track windows in Elements tab (dialog, popups, etc)
 * Fix #614: Add the ability to manually control Runtime class object tracking (for 
   J2V8 integration)
 * Fix #612/613: Fix issue subclassing Runtime class and other devtools domain modules
 * Fix #602: Make it possible to change welcome banner (see Page class constructor)
 * Fix #600: Actually make .remove() method work properly in DefaultDumperPluginsBuilder
 * Fix #596: Sort SharedPreferences entries
 * Fix #623: dumpapp now supports ADB_SERVER_SOCKET variable
 * Fix #589: Fix parsing issue using dumpapp script causing failure to connect
 * Fix #529: WebSocket session info now displayed correctly on upgrade
 * Fix #541/580: Avoid crash when installing the Stetho interceptor in the 
   wrong okhttp chain (the request chain lacks a connection instance)

## Version 1.5.0
_2017_04_13_

 * **Generic socket support**
   Added the ability to inspect arbitrary sockets via Chrome devtools'
   WebSocket APIs.  This support is integrated into stetho-sample
   as a demo, and in future releases we'd like to make it available
   generically for at least okhttp customers.

 * **Major Elements tab extensibility points**
   Significant expansion to AndroidDocumentProviderFactory which allows 
   for a much greater degree of customization, including the
   ability to have editable styles.  Over time we'd like to incorporate
   this support into native Android UI but also to make it even
   easier to unlock powerful new customizations for custom view or
   entire rendering systems.

 * Fix #392: Fix database id mapping for multiple database providers
 * Fix #521: Fix hit testing for empty view groups
 * Fix #513: Fix LeakCanary false positive
 * Fix #514: Handle runtime exceptions from database drivers
 * Fix #511: Remove accidental hard dependency on support library
 * Fix #406: Fix stetho-rhino importClass/Package not working properly
 * Fix #499: Treat HttpURLConnection headers as case insensitive
 * Fix #512: Fix severe stdin issue in dumpapp script
 * Fix #486: Fix TextViewDescriptor NPE when text is null

## Version 1.4.2
_2016_12_14_

 * **Bug fixes**
  Fixes a few bugs in the Elements tab.

 * Fix #381: Fixes NPE while rotating the device with retained fragments.
 * Fix #447: Support Instant Run in android studio, by fixing ObjectMapper
 * Fix #456: Support ANDROID_ADB_SERVER_PORT
 * Fix #454: Upgrade to OkHttp 3.4.2
 * Fix #449: Make sure only unfocusable children's descriptions are being
   co-opted by parents


## Version 1.4.1
_2016_09_13_

  * Fix #432 Have DefaultDatabaseProvider return filename

    v1.4.0 exposed a long standing bug relating to loading databases.


## Version 1.4.0
_2016_09_07_

 * **Add UI Accessibility Properties to Styles tab**
   Added support for accessibility inspection, which allows users to select
   a View and see whether or not it will be focusable by an Accessibility
   Service, why it will or won't be focusable, the text description sent to
   Accessibility Services, and any AccessibilityActions that are currently
   available on the View.

 * Fix #367: Fixed SqliteDatabaseDriver with custom DatabaseFilesProvider
 * Fix #424: Make aar be the default packaging in maven

## Version 1.3.1

_2016-02-25_

 * **Major performance improvements in Elements tab**
   Several performance, correctness, and stability improvements related to
   how Stetho performs tree diffing.

 * Fix #349: Fix dumpapp scripts under various edge cases.
 * Fix #357: Remove static fields from exported view "styles".

## Version 1.3.0

_2016-01-20_

 * **okhttp3 support!**
   A new module, stetho-okhttp3, has been added which supports the new
   okhttp3 APIs.  Note that stetho-okhttp is now deprecated.

 * **Removed Apache HttpClient dependency**
   A new, lightweight HTTP server implementation replaces it in Stetho
   and the dumpapp protocol has been modified to no longer use HTTP.
   Old dumpapp scripts will still work with new clients, however the
   opposite will hang!

 * **Custom database drivers**
   Completely custom or ContentProvider-based database drivers are available
   which allow greater inspection options with some configuration.  See
   `DefaultInspectorModulesBuilder#provideDatabaseDriver`.

 * New #282: Show view margins in "Styles" subtab.
 * New #289: Show SQLite views as tables.
 * New #294: dumpapp now responds to `STETHO_PROCESS` env variable in addition
   to the `--process` flag.
 * Fix #286: Minor JsConsole improvements.
 * Fix #297: Sort CSS properties by name.
 * Fix #292: Minor JSON serialization fixes.
 * Fix #299: Memory leak fixes in view inspection (still some likely remain).
 * Fix #305: Add proguard rules to stetho-js-rhino aar artifact.
 * Fix #313: Work around issues formatting `Long.MIN_VALUE` and possibly others
   when showing in the database view.
 * Fix #332: NPE in ShadowDocument.getGarbageElements().

## Version 1.2.0

_2015-09-11_

 * **View properties support!**
   The "Styles" and "Computed" sub-tabs in "Elements" are now implemented,
   complete with the box model diagram and a summary of the most useful view
   properties.

 * **Screencasting**
   Click the small screen icon in the upper right to view a live preview of
   your phone's screen while using Stetho!  Coming soon: mouse/keyboard
   support.

 * **Console tab support**
   Arbitrary Java/JavaScript support added to the Console with the optional
   `stetho-js-rhino` dependency.  See
   [`stetho-sample/build.gradle`](stetho-sample/build.gradle) for details.

 * **New simpler initialization and customization API**
   Most callers can now just use `Stetho.initializeWithDefaults(context)`.

 * New #218: Ability to pass pretty printers for binary data in the Network tab.
 * New #248: Implement transparent request decompression.
 * New #225: Ability to search View hierarchy (invoke with CTRL+F on the Elements tab).
 * New #238: Add EXPLAIN support in SQL console.
 * New #222: Add PRAGMA support in SQL console. 
 * New #207: Add `dumpapp files` plugin.
 * New #181: Highlight view margins and padding when hovering over DOM entry.
 * New #211: Implement DialogFragment in Elements tab.
 * Fix #231: Sort database and shared preferences entries by name.
 * Fix #206: Fix small memory leak in View hierarchy support.
 * Fix #204: Use DOM tree diffing to fix ListView and a number of other edge
   case view hierarchies.
 * Fix #183: Fix Fragment support in Elements tab.

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
