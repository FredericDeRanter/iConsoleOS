# ScrollableNumberPicker (vendored)

Copy of `com.github.michaelmuenzer:ScrollableNumberPicker:0.2.2`
(https://github.com/michaelmuenzer/ScrollableNumberPicker, see the license there), taken from
the published sources jar and aar.

Why: the published library was built against the old Android support library
(`android.support.v4.*`). The toolchain no longer runs Jetifier, so the app crashed with
`NoClassDefFoundError: android/support/v4/content/ContextCompat` as soon as the profile editor
(`CustomTimePicker2`) was opened.

The only change from upstream is the support-library imports, which now point to AndroidX
(`androidx.annotation`, `androidx.core`). The package, class names, attributes and resources
are unchanged, so the app's layouts keep working as they were.
