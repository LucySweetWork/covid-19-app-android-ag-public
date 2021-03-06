package uk.nhs.nhsx.covid19.android.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import kotlinx.android.synthetic.scenarios.activity_debug.buttonFeatureFlags
import kotlinx.android.synthetic.scenarios.activity_debug.environmentSpinner
import kotlinx.android.synthetic.scenarios.activity_debug.exposureNotificationMocks
import kotlinx.android.synthetic.scenarios.activity_debug.fieldTests
import kotlinx.android.synthetic.scenarios.activity_debug.languageSpinner
import kotlinx.android.synthetic.scenarios.activity_debug.scenarioOnboarding
import kotlinx.android.synthetic.scenarios.activity_debug.scenario_main
import kotlinx.android.synthetic.scenarios.activity_debug.screenButtonContainer
import kotlinx.android.synthetic.scenarios.activity_debug.statusScreen
import uk.nhs.nhsx.covid19.android.app.about.EditPostalDistrictActivity
import uk.nhs.nhsx.covid19.android.app.about.MoreAboutAppActivity
import uk.nhs.nhsx.covid19.android.app.about.UserDataActivity
import uk.nhs.nhsx.covid19.android.app.common.EnableBluetoothActivity
import uk.nhs.nhsx.covid19.android.app.common.EnableExposureNotificationsActivity
import uk.nhs.nhsx.covid19.android.app.common.EnableLocationActivity
import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.edgecases.DeviceNotSupportedActivity
import uk.nhs.nhsx.covid19.android.app.edgecases.TabletNotSupportedActivity
import uk.nhs.nhsx.covid19.android.app.exposure.ShareKeysInformationActivity
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EncounterDetectionActivity
import uk.nhs.nhsx.covid19.android.app.featureflag.testsettings.TestSettingsActivity
import uk.nhs.nhsx.covid19.android.app.fieldtests.FieldTestsActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.DataAndPrivacyActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.WelcomeActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeHelpActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.CameraPermissionNotGranted
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.InvalidContent
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.ScanningNotSupported
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.Success
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResultActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.QrScannerActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.NoSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsAdviceIsolateActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireActivity
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.GREEN
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationActivity
import uk.nhs.nhsx.covid19.android.app.status.IsolationPaymentActivity
import uk.nhs.nhsx.covid19.android.app.status.RiskLevelActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultActivity
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultActivity
import java.time.LocalDate

class DebugActivity : AppCompatActivity(R.layout.activity_debug) {

    private lateinit var debugSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        setSupportActionBar(toolbar)

        debugSharedPreferences = getSharedPreferences(DEBUG_PREFERENCES_NAME, Context.MODE_PRIVATE)

        setupEnvironmentSpinner()

        setupFeatureFlagButton()

        setupExposureNotificationCheckbox()

        setupLanguageSpinner()

        setupScenariosButtons()

        setupScreenButtons()
    }

    private fun setupEnvironmentSpinner() {
        val environments = scenariosApp.environments
        val environmentsAdapter = EnvironmentAdapter(this, environments)

        environmentSpinner.adapter = environmentsAdapter
        val selectedEnvironment = debugSharedPreferences.getInt(SELECTED_ENVIRONMENT, 0)
            .coerceIn(0, environments.size - 1)
        environmentSpinner.setSelection(selectedEnvironment)

        environmentSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                debugSharedPreferences.edit().putInt(SELECTED_ENVIRONMENT, position).apply()
                scenariosApp.updateDependencyGraph()
            }
        }
    }

    private fun setupExposureNotificationCheckbox() {
        val useMockedExposureNotifications =
            debugSharedPreferences.getBoolean(USE_MOCKED_EXPOSURE_NOTIFICATION, false)
        exposureNotificationMocks.isChecked = useMockedExposureNotifications

        exposureNotificationMocks.setOnCheckedChangeListener { _, isChecked ->
            debugSharedPreferences.edit().putBoolean(USE_MOCKED_EXPOSURE_NOTIFICATION, isChecked)
                .apply()
            scenariosApp.updateDependencyGraph()
        }
    }

    private fun setupLanguageSpinner() {
        val supportedLanguages = SupportedLanguage.values().toList()
        val languageAdapter = LanguageAdapter(this, supportedLanguages)

        languageSpinner.adapter = languageAdapter

        val previouslySelectedLanguage = debugSharedPreferences.getString(SELECTED_LANGUAGE, null)
        val indexOfPreviouslySelectedLanguage =
            supportedLanguages.map { it.code }.indexOf(previouslySelectedLanguage)
        languageSpinner.setSelection(indexOfPreviouslySelectedLanguage)

        languageSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedLanguage = supportedLanguages[position]
                debugSharedPreferences.edit()
                    .putString(SELECTED_LANGUAGE, selectedLanguage.code).apply()

                scenariosApp.updateDependencyGraph()
            }
        }
    }

    private fun setupFeatureFlagButton() {
        buttonFeatureFlags.setOnClickListener {
            startActivity(Intent(this, TestSettingsActivity::class.java))
        }
    }

    private fun setupScenariosButtons() {
        scenario_main.setOnClickListener {
            MainActivity.start(this)
        }

        scenarioOnboarding.setOnClickListener {
            WelcomeActivity.start(this)
        }

        fieldTests.setOnClickListener {
            startActivity<FieldTestsActivity>()
        }

        statusScreen.setOnClickListener {
            startActivity<StatusActivity>()
        }
    }

    private fun setupScreenButtons() {
        addScreenButton("Isolation Payment") {
            startActivity<IsolationPaymentActivity>()
        }
        addScreenButton("Post code") {
            PostCodeActivity.start(this)
        }

        addScreenButton("Data and privacy") {
            DataAndPrivacyActivity.start(this)
        }

        addScreenButton("Permission") {
            PermissionActivity.start(this)
        }

        addScreenButton("Enable bluetooth") {
            EnableBluetoothActivity.start(this)
        }

        addScreenButton("Enable location services") {
            EnableLocationActivity.start(this)
        }

        addScreenButton("Enable exposure notifications") {
            EnableExposureNotificationsActivity.start(this)
        }

        addScreenButton("QR Code Scanner") {
            startActivity<QrScannerActivity>()
        }

        addScreenButton("QR Code Scan Success") {
            QrCodeScanResultActivity.start(this, Success("Sample Venue"))
        }

        addScreenButton("QR Code Scan Failure") {
            QrCodeScanResultActivity.start(this, InvalidContent)
        }

        addScreenButton("QR Code Permission Not Granted") {
            QrCodeScanResultActivity.start(this, CameraPermissionNotGranted)
        }

        addScreenButton("QR Code Scanning Not Supported") {
            QrCodeScanResultActivity.start(this, ScanningNotSupported)
        }

        addScreenButton("QR Code Help") {
            startActivity<QrCodeHelpActivity>()
        }

        addScreenButton("Risky Venue Alert") {
            VenueAlertActivity.start(this, "ABCD1234")
        }

        addScreenButton("Questionnaire screen") {
            startActivity<QuestionnaireActivity>()
        }

        addScreenButton("Questionnaire No Symptoms") {
            startActivity<NoSymptomsActivity>()
        }

        addScreenButton("Questionnaire Isolation Advice") {
            SymptomsAdviceIsolateActivity.start(this, true, 14)
        }

        addScreenButton("Testing information") {
            startActivity<TestOrderingActivity>()
        }

        addScreenButton("Test result") {
            startActivity<TestResultActivity>()
        }

        addScreenButton("Encounter detection") {
            EncounterDetectionActivity.start(this)
        }

        addScreenButton("Isolation Expiration") {
            IsolationExpirationActivity.start(this, LocalDate.now().minusDays(1).toString())
        }

        addScreenButton("More about the app") {
            MoreAboutAppActivity.start(this)
        }

        addScreenButton("User Data") {
            UserDataActivity.start(this)
        }

        addScreenButton("Device not supported") {
            startActivity<DeviceNotSupportedActivity>()
        }

        addScreenButton("Tablet not supported") {
            startActivity<TabletNotSupportedActivity>()
        }

        addScreenButton("Share keys information") {
            startActivity<ShareKeysInformationActivity>()
        }

        addScreenButton("Risk level") {
            RiskLevelActivity.start(
                this,
                RiskyPostCodeViewState.Risk(
                    "CM2",
                    RiskIndicator(
                        colorScheme = GREEN,
                        name = Translatable(mapOf("en" to "Tier1")),
                        heading = Translatable(mapOf("en" to "Data from the NHS shows that the spread of coronavirus in your area is low.")),
                        content = Translatable(
                            mapOf(
                                "en" to "Your local authority has normal measures for coronavirus in place. It’s important that you continue to follow the latest official government guidance to help control the virus.\n" +
                                    "\n" +
                                    "Find out the restrictions for your local area to help reduce the spread of coronavirus."
                            )
                        ),
                        linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                        linkUrl = Translatable(mapOf("en" to "https://faq.covid19.nhs.uk/article/KA-01270/en-us"))
                    )
                )
            )
        }

        addScreenButton("Edit post code") {
            EditPostalDistrictActivity.start(this)
        }

        addScreenButton("Link test result") {
            startActivity<LinkTestResultActivity>()
        }

        addScreenButton("Show all notifications") {
            val notifications = app.appComponent.provideNotificationProvider()
            notifications.showPotentialExposureExplanationNotification()
            notifications.showAppIsAvailable()
            notifications.showAppIsNotAvailable()
            notifications.showAreaRiskChangedNotification()
            notifications.showExposureNotification()
            notifications.showExposureNotificationReminder()
            notifications.showRiskyVenueVisitNotification()
            notifications.showStateExpirationNotification()
            notifications.showTestResultsReceivedNotification()
        }
    }

    private fun addScreenButton(
        title: String,
        action: () -> Unit
    ) {
        val button = Button(ContextThemeWrapper(this, R.style.PrimaryButton))
        button.text = title
        button.setOnClickListener { action() }
        screenButtonContainer.addView(button)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_debug, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.actionClearAppData -> {
                (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val DEBUG_PREFERENCES_NAME = "debugPreferences"
        const val SELECTED_ENVIRONMENT = "SELECTED_ENVIRONMENT"
        const val SELECTED_LANGUAGE = "SELECTED_LANGUAGE"
        const val USE_MOCKED_EXPOSURE_NOTIFICATION = "USE_MOCKED_EXPOSURE_NOTIFICATION"

        fun start(context: Context) = context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, DebugActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
    }
}
