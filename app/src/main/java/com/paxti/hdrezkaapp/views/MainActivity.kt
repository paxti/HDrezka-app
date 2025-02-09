package com.paxti.hdrezkaapp.views

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.algolia.instantsearch.voice.VoiceSpeechRecognizer
import com.algolia.instantsearch.voice.ui.Voice
import com.algolia.instantsearch.voice.ui.Voice.shouldExplainPermission
import com.algolia.instantsearch.voice.ui.Voice.showPermissionRationale
import com.algolia.instantsearch.voice.ui.VoicePermissionDialogFragment
import com.chivorn.smartmaterialspinner.SmartMaterialSpinner
import com.paxti.hdrezkaapp.BuildConfig
import com.paxti.hdrezkaapp.R
import com.paxti.hdrezkaapp.clients.PlayerJsInterface
import com.paxti.hdrezkaapp.constants.DeviceType
import com.paxti.hdrezkaapp.constants.DownloadType
import com.paxti.hdrezkaapp.constants.NavigationMenuTabs
import com.paxti.hdrezkaapp.constants.UpdateItem
import com.paxti.hdrezkaapp.controllers.DownloadController
import com.paxti.hdrezkaapp.controllers.SocketFactory
import com.paxti.hdrezkaapp.interfaces.IConnection
import com.paxti.hdrezkaapp.interfaces.IPagerView
import com.paxti.hdrezkaapp.interfaces.NavigationMenuCallback
import com.paxti.hdrezkaapp.interfaces.OnFragmentInteractionListener
import com.paxti.hdrezkaapp.interfaces.OnFragmentInteractionListener.Action
import com.paxti.hdrezkaapp.objects.Film
import com.paxti.hdrezkaapp.objects.SettingsData
import com.paxti.hdrezkaapp.objects.UserData
import com.paxti.hdrezkaapp.utils.ConnectionManager.isInternetAvailable
import com.paxti.hdrezkaapp.utils.ConnectionManager.showConnectionErrorDialog
import com.paxti.hdrezkaapp.utils.DialogManager
import com.paxti.hdrezkaapp.utils.FragmentOpener
import com.paxti.hdrezkaapp.views.fragments.*
import com.paxti.hdrezkaapp.views.tv.NavigationMenu
import com.paxti.hdrezkaapp.views.tv.interfaces.FragmentChangeListener
import com.paxti.hdrezkaapp.views.tv.interfaces.NavigationStateListener
import com.google.firebase.FirebaseApp
import com.jakewharton.processphoenix.ProcessPhoenix
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity(), OnFragmentInteractionListener, IConnection, IPagerView, FragmentChangeListener,
    NavigationMenuCallback, VoiceSpeechRecognizer.ResultsListener, NavigationStateListener {
    private var isSettingsOpened: Boolean = false
    private var isSeriesUpdatesOpened: Boolean = false
    private var mainFragment: Fragment? = null
    private lateinit var currentFragment: Fragment
    private var savedInstanceState: Bundle? = null
    private var apkUrl: String? = null
    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null

    /* TV */
    private lateinit var navMenuFragment: NavigationMenu
    private lateinit var navFragmentLayout: FrameLayout
    private var doubleBackToExitPressedOnce = false

    override fun attachBaseContext(context: Context?) {
        super.attachBaseContext(context)

        if (context != null) {
            SettingsData.initDeviceType(context)

            val uiMode: Int? = if (SettingsData.deviceType == DeviceType.TV) {
                Configuration.UI_MODE_TYPE_TELEVISION
            } else if (SettingsData.deviceType == DeviceType.MOBILE) {
                Configuration.UI_MODE_TYPE_NORMAL
            } else {
                null
            }

            if (uiMode != null) {
                resources.configuration.uiMode = uiMode
                resources.configuration.setTo(resources.configuration)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.savedInstanceState = savedInstanceState

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                downloadApk()
            } else {
                Toast.makeText(this, getString(R.string.perm_write_hint), Toast.LENGTH_LONG).show()
            }
        }

        if (detectUIMode()) {
            setContentView(R.layout.activity_main)

            initCreate()
        } else {
            setContentView(R.layout.activity_device_chooser)

            SettingsData.setUIMode(DeviceType.TV, this)
            refreshActivity()
        }
    }

    private fun initCreate() {
        if (isInternetAvailable(applicationContext)) {
            FirebaseApp.initializeApp(this)

            try {
                setDefaultSSLSocketFactory(SocketFactory())
            } catch (e: Exception) {
                e.printStackTrace()
            }

            initApp()

            checkAppVersion()
        } else {
            showConnectionError(IConnection.ErrorType.NO_INTERNET, "")
        }
    }

    private fun detectUIMode(): Boolean {
        return SettingsData.deviceType != null
    }

    fun refreshActivity() {
        ProcessPhoenix.triggerRebirth(this)
    }

    override fun showConnectionError(type: IConnection.ErrorType, errorText: String) {
        try {
            showConnectionErrorDialog(this, type, ::initCreate)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun initApp() {
        if (savedInstanceState == null) {
            SettingsData.init(applicationContext)
            UserData.init(applicationContext)

            if (SettingsData.isInitHint == false) {
                showInitHint()
            }

            if (SettingsData.deviceType == DeviceType.MOBILE) {
                // Mobile
                requestedOrientation = if (SettingsData.isAutorotate == true) {
                    ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                mainFragment = ViewPagerFragment()
                onFragmentInteraction(null, mainFragment, Action.NEXT_FRAGMENT_REPLACE, false, null, null, null, null)

                createUserMenu()
                setUserAvatar()

                if (intent.data != null) {
                    val link = SettingsData.provider + intent.data.toString().replace("${intent.data!!.scheme}://", "").replace(intent.data!!.host ?: "", "")
                    FragmentOpener.openWithData(mainFragment, this, Film(link), "film")
                }
            } else {
                // TV
                navFragmentLayout = findViewById(R.id.nav_fragment)

                navMenuFragment = NavigationMenu()
                supportFragmentManager.beginTransaction().replace(R.id.nav_fragment, navMenuFragment).commit()

                SettingsData.mainScreen?.let {
                    mainFragment = when (it) {
                        0 -> NewestFilmsFragment()
                        1 -> CategoriesFragment()
                        2 -> SearchFragment()
                        3 -> BookmarksFragment()
                        4 -> WatchLaterFragment()
                        else -> NewestFilmsFragment()
                    }
                }

                fun fragmentInit() {

                }

                onFragmentInteraction(null, mainFragment, Action.NEXT_FRAGMENT_REPLACE, false, null, null, null, ::fragmentInit)
            }
        }
    }

    private fun showInitHint() {
        val builder = DialogManager.getDialog(this, R.string.init_hint_title)
        builder.setMessage(R.string.init_hint_msg)
        builder.setPositiveButton(getString(R.string.ok_text)) { dialog, id ->
            SettingsData.updateInitHint(this)
        }
        val d = builder.create()
        d.show()
    }

    fun showProviderEnter() {
        val builder = DialogManager.getDialog(this, R.string.provider_enter_title)
        val dialogView = layoutInflater.inflate(R.layout.dialog_provider_enter, null)
        val spinner = dialogView.findViewById<SmartMaterialSpinner<String>>(R.id.dialog_provider_protocol)
        val editText = dialogView.findViewById<EditText>(R.id.dialog_provider_enter)
        val adapter: ArrayAdapter<*> = ArrayAdapter.createFromResource(this, R.array.providerProtocols, android.R.layout.simple_spinner_item)
        var selectedProtocol = ""
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, itemSelected: View?, selectedItemPosition: Int, selectedId: Long) {
                val arr = resources.getStringArray(R.array.providerProtocols)
                selectedProtocol = arr[selectedItemPosition]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinner.setSelection(0)

        builder.setNegativeButton(getString(R.string.exit)) { dialog, id ->
            exitProcess(0)
        }
        builder.setPositiveButton(getString(R.string.ok), null)
        builder.setView(dialogView)
        builder.setCancelable(false)
        val d = builder.create()
        d.show()

        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val enteredText = editText.text.toString().replace(" ", "").replace("\n", "")

            if (enteredText.isNotEmpty()) {
                val link = selectedProtocol + enteredText
                Toast.makeText(this, getString(R.string.new_provider, link), Toast.LENGTH_LONG).show()
                SettingsData.setProvider(link, this, true)
                d.cancel()
                initApp()
            } else {
                Toast.makeText(this, getString(R.string.empty_provider), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createUserMenu() {
        if (SettingsData.deviceType != DeviceType.TV) {
            findViewById<ImageView>(R.id.activity_main_iv_user).setOnClickListener {
                openUserMenu()
            }
        }
    }

    private fun openUserMenu() {
        if (!isSettingsOpened) {
            val f = if (mainFragment?.isVisible == true) mainFragment
            else currentFragment
            onFragmentInteraction(f, SettingsFragment(), Action.NEXT_FRAGMENT_HIDE, true, null, null, null, null)
            isSettingsOpened = true
        }
    }

    fun setUserAvatar() {
        if (SettingsData.deviceType != DeviceType.TV) {
            val imageView: ImageView = findViewById(R.id.activity_main_iv_user)
            if (UserData.avatarLink != null && UserData.avatarLink!!.isNotEmpty()) {
                Picasso.get().load(UserData.avatarLink).into(imageView)
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.no_avatar))
            }
        }
    }

    override fun updatePager() {
        if (SettingsData.deviceType != DeviceType.TV) {
            if (mainFragment is ViewPagerFragment) {
                (mainFragment as ViewPagerFragment).setAdapter()
            }
        }
    }

    override fun redrawPage(item: UpdateItem) {
        if (SettingsData.deviceType != DeviceType.TV) {
            if (mainFragment is ViewPagerFragment) {
                (mainFragment as ViewPagerFragment).updatePage(item)
            }
        }
    }

    override fun onFragmentInteraction(fragmentSource: Fragment?, fragmentReceiver: Fragment?, action: Action, isBackStack: Boolean, backStackTag: String?, data: Bundle?, callback: (() -> Unit)?, init: (() -> Unit)?) {
        val fTrans: FragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentReceiver?.arguments = data

        val animIn = R.anim.fade_in
        val animOut = R.anim.fade_out
        fTrans.setCustomAnimations(animIn, animOut, animIn, animOut)

        when (action) {
            Action.NEXT_FRAGMENT_HIDE -> {
                if (mainFragment?.isVisible == true) fTrans.hide(mainFragment!!)
                else fragmentSource?.let { fTrans.hide(it) }

                val frags: List<Fragment> = supportFragmentManager.fragments
                var f: Fragment? = null
                for (fragment in frags) {
                    if (fragment == fragmentReceiver) {
                        f = fragment
                        break
                    }
                }
                if (fragmentReceiver != null) {
                    currentFragment = fragmentReceiver
                }

                if (f == null) {
                    if (fragmentReceiver != null) {
                        fTrans.add(R.id.activity_main_fcv_container, fragmentReceiver)
                    }
                } else {
                    if (fragmentReceiver != null) {
                        fTrans.show(fragmentReceiver)
                    }
                }

                if (isBackStack) {
                    fTrans.addToBackStack(backStackTag)
                }
                fTrans.commit()
            }
            Action.NEXT_FRAGMENT_REPLACE -> {
                if (fragmentReceiver != null) {
                    fTrans.replace(R.id.activity_main_fcv_container, fragmentReceiver)
                }
                if (isBackStack) {
                    fTrans.addToBackStack(backStackTag)
                }
                fTrans.commit()
                supportFragmentManager.executePendingTransactions()
                if (init != null) {
                    init()
                }
            }
            Action.POP_BACK_STACK -> supportFragmentManager.popBackStack()
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (callback != null) {
                callback()
            }
        }
    }

    override fun findFragmentByTag(tag: String): Fragment? {
        return supportFragmentManager.findFragmentByTag(tag)
    }

    override fun onBackPressed() {
        if (isSettingsOpened) {
            isSettingsOpened = false
        }

        if (isSeriesUpdatesOpened) {
            isSeriesUpdatesOpened = false
        }

        if (SettingsData.deviceType == DeviceType.TV) {
            val acceptableFragment = when (mainFragment) {
                is NewestFilmsFragment -> true
                is CategoriesFragment -> true
                is SearchFragment -> true
                is BookmarksFragment -> true
                is WatchLaterFragment -> true
                is SettingsFragment -> true
                else -> false
            }

            if (acceptableFragment && mainFragment?.isVisible == true) {
                if (doubleBackToExitPressedOnce) {
                    super.onBackPressed()
                    return
                }

                this.doubleBackToExitPressedOnce = true
                Toast.makeText(this, getString(R.string.double_click_hint), Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
            } else {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    /* TV */
    override fun switchFragment(fragmentName: String?) {
        var fragmentTo: Fragment? = null

        when (fragmentName) {
            NavigationMenuTabs.nav_menu_newest -> {
                fragmentTo = NewestFilmsFragment()
            }
            NavigationMenuTabs.nav_menu_categories -> {
                fragmentTo = CategoriesFragment()
            }
            NavigationMenuTabs.nav_menu_search -> {
                fragmentTo = SearchFragment()
            }
            NavigationMenuTabs.nav_menu_bookmarks -> {
                fragmentTo = BookmarksFragment()
            }
            NavigationMenuTabs.nav_menu_later -> {
                fragmentTo = WatchLaterFragment()
            }
            NavigationMenuTabs.nav_menu_settings -> {
                fragmentTo = SettingsFragment()
            }
        }

        if (fragmentTo != null) {
            mainFragment = fragmentTo
            onFragmentInteraction(null, fragmentTo, Action.NEXT_FRAGMENT_REPLACE, false, null, null, null, null)
        }
    }

    override fun navMenuToggle(toShow: Boolean) {
    }

    override fun onAttachFragment(fragment: Fragment) {
        when (fragment) {
            is NavigationMenu -> {
                fragment.setFragmentChangeListener(this)
                fragment.setNavigationStateListener(this)
            }
        }
    }

    override fun onDestroy() {
        PlayerJsInterface.notifyanager?.cancel(0)
        super.onDestroy()
    }

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    override fun onResults(possibleTexts: Array<out String>) {
        val spokenText = possibleTexts.firstOrNull() //?.capitalize()
        if (mainFragment is SearchFragment) {
            (mainFragment as SearchFragment).showVoiceResult(spokenText)
        } else if (mainFragment is ViewPagerFragment) {
            (mainFragment as ViewPagerFragment).showVoiceCommand(spokenText)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (Voice.isRecordPermissionWithResults(requestCode, grantResults)) {
            when {
                Voice.isPermissionGranted(grantResults) -> {
                    if (mainFragment is SearchFragment) {
                        (mainFragment as SearchFragment).showVoiceDialog()
                    } else if (mainFragment is ViewPagerFragment) {
                        (mainFragment as ViewPagerFragment).setVoiceCommand()
                    }
                }
                shouldExplainPermission() -> showPermissionRationale(getPermissionView())
                else -> Voice.showPermissionManualInstructions(getPermissionView())
            }
        }
    }

    private fun getPermissionView(): View = getPermissionDialog()!!.requireView().findViewById(R.id.positive)
    private fun getPermissionDialog() = supportFragmentManager.findFragmentByTag(SearchFragment.Tag.getTag().name) as? VoicePermissionDialogFragment

    private fun downloadApk() {
        if (apkUrl != null && apkUrl!!.isNotEmpty()) {
            val builder = DialogManager.getDialog(this, null, false)
            val dialogView = layoutInflater.inflate(R.layout.dialog_progress, null, false)
            val progressbarView = dialogView.findViewById<ProgressBar>(R.id.progressbar)
            val hintView = dialogView.findViewById<TextView>(R.id.hint_text)
            builder.setView(dialogView)
            val d = builder.create()
            d.show()

            val percentView = dialogView.findViewById<TextView>(R.id.percent)
            val totalView = dialogView.findViewById<TextView>(R.id.totalSize)
            var totalSize = 0F
            var totalText = ""

            fun onComplete(type: DownloadType, value: Float) {
                when (type) {
                    DownloadType.SET_MAX -> {
                        progressbarView.max = value.toInt() * 1024
                        totalSize = value
                        totalText = String.format("%.2f", value)
                        totalView.text = "0/$totalText МБ"
                    }
                    DownloadType.SET_PROGRESS -> {
                        progressbarView.progress = value.toInt() * 1024
                        val valueText = String.format("%.2f", value)
                        percentView.text = "${(((value) * 1024F).toInt() * 100) / (totalSize * 1024F).toInt()}%"
                        totalView.text = "$valueText/$totalText МБ"
                    }
                    DownloadType.SUCCESS -> {
                        d.dismiss()
                    }
                    DownloadType.FAILED -> {
                        d.setCancelable(true)
                        progressbarView.visibility = View.GONE
                        percentView.visibility = View.GONE
                        totalView.visibility = View.GONE
                        hintView.visibility = View.VISIBLE
                    }
                }
            }

            DownloadController(this, apkUrl!!, ::onComplete).enqueueDownload()
        }
    }

    private fun checkAppVersion() {
        if (SettingsData.isCheckNewVersion == true) {
            val _context = this

            GlobalScope.launch {
                try {
                    val uri: URI = URI.create(SettingsData.UPDATE_URL)
                    uri.toURL().openStream().use { inputStream ->
                        val versionString = convertInputStreamToString(inputStream)
                        try {
                            if (versionString != null) {
                                val versionObject = JSONObject(versionString)
                                val versionCode = versionObject.getInt("code")
                                val serverVersion = versionObject.getString("version")
                                val newFeatures = versionObject.getString("newFeatures")
                                apkUrl = versionObject.getString("apk")

                                val isNewVersion = compareAppVersion(versionCode)

                                withContext(Dispatchers.Main) {
                                    if (isNewVersion) {
                                        // show dialog
                                        val builder = DialogManager.getDialog(_context, R.string.new_version_hint)
                                        builder.setNegativeButton(R.string.cancel) { d, i ->
                                            d.dismiss()
                                        }

                                        builder.setPositiveButton(R.string.update_to) { d, i ->
                                            when (PackageManager.PERMISSION_GRANTED) {
                                                ContextCompat.checkSelfPermission(_context, Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                                                    downloadApk()
                                                }
                                                else -> {
                                                    requestPermissionLauncher?.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                                }
                                            }
                                        }

                                        builder.setMessage("${BuildConfig.VERSION_NAME} -> $serverVersion\n\n $newFeatures")
                                        val d = builder.create()
                                        d.show()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun compareAppVersion(version: Int?): Boolean {
        if (version == null) {
            throw Exception("File not found")
        }

        return BuildConfig.VERSION_CODE < version
    }

    private fun convertInputStreamToString(inputStream: InputStream): String? {
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var length: Int
        while (inputStream.read(buffer).also { length = it } != -1) {
            result.write(buffer, 0, length)
        }

        return result.toString(StandardCharsets.UTF_8.name())
    }

    override fun onStateChanged(closed: Boolean) {
        val closedWidth = 0
        val expandedWidth = 200

        val anim = if (closed) {
            ValueAnimator.ofInt(expandedWidth, closedWidth)
        } else {
            ValueAnimator.ofInt(closedWidth, expandedWidth)
        }

        anim.interpolator = AccelerateInterpolator()
        anim.duration = NavigationMenu.animDuration
        anim.addUpdateListener { animation ->
            findViewById<FrameLayout>(R.id.activity_main_fcv_container).translationX = (animation.animatedValue as Int).toFloat()
        }
        anim.start()
    }
}