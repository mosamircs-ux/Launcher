package com.novapulse.launcher.activities

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.novapulse.launcher.BuildConfig
import com.novapulse.launcher.R
import com.novapulse.launcher.ads.AdManager
import com.novapulse.launcher.databinding.ActivitySettingsBinding
import com.novapulse.launcher.extensions.config
import com.novapulse.launcher.extensions.isDefaultLauncher
import com.novapulse.launcher.helpers.MAX_COLUMN_COUNT
import com.novapulse.launcher.helpers.MAX_ROW_COUNT
import com.novapulse.launcher.helpers.MIN_COLUMN_COUNT
import com.novapulse.launcher.helpers.MIN_ROW_COUNT
import com.novapulse.launcher.receivers.LockDeviceAdminReceiver
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.isTiramisuPlus
import org.fossify.commons.models.RadioItem
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {

    private val binding by viewBinding(ActivitySettingsBinding::inflate)
    private var settingsBannerAdView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupEdgeToEdge(padBottomSystem = listOf(binding.settingsAdWrapper))
        setupMaterialScrollListener(binding.settingsNestedScrollview, binding.settingsAppbar)
        setupOptionsMenu()
        setupBrandHeroHeader()

        binding.settingsAdContainer.post {
            settingsBannerAdView = AdManager.loadAdaptiveBanner(
                activity = this,
                container = binding.settingsAdContainer,
                adUnitId = getString(R.string.admob_banner_settings_id)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        settingsBannerAdView?.resume()
        setupTopAppBar(binding.settingsAppbar, NavigationIcon.Arrow)
        updateDefaultLauncherPill()

        setupCustomizeColors()
        setupUseEnglish()
        setupDoubleTapToLock()
        setupCloseAppDrawerOnOtherAppOpen()
        setupOpenKeyboardOnAppDrawer()
        setupDrawerColumnCount()
        setupDrawerSearchBar()
        setupShowDrawerAppLabels()
        setupHomeRowCount()
        setupHomeColumnCount()
        setupShowHomeAppLabels()
        setupLanguage()
        setupManageHiddenIcons()
        updateTextColors(binding.settingsHolder)

        // Preserve NovaPulse brand hero and cyan section accent styling
        val cyanAccent = ContextCompat.getColor(this, R.color.novapulse_accent_cyan)
        val primaryText = ContextCompat.getColor(this, R.color.novapulse_text_primary)
        val secondaryText = ContextCompat.getColor(this, R.color.novapulse_text_secondary)

        binding.settingsBrandTitle.setTextColor(primaryText)
        binding.settingsBrandVersion.setTextColor(cyanAccent)
        binding.settingsBrandTagline.setTextColor(secondaryText)
        binding.settingsDefaultLauncherPill.setTextColor(primaryText)

        arrayOf(
            binding.settingsColorCustomizationSectionLabel,
            binding.settingsGeneralSettingsLabel,
            binding.settingsDrawerSettingsLabel,
            binding.settingsHomeScreenLabel
        ).forEach {
            it.setTextColor(cyanAccent)
        }
    }

    override fun onPause() {
        settingsBannerAdView?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        settingsBannerAdView?.destroy()
        settingsBannerAdView = null
        super.onDestroy()
    }

    private fun setupBrandHeroHeader() {
        binding.settingsBrandVersion.text = "v${BuildConfig.VERSION_NAME}"
        binding.settingsDefaultLauncherPill.setOnClickListener {
            val intents = listOf(
                Intent(Settings.ACTION_HOME_SETTINGS),
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
                Intent(Settings.ACTION_SETTINGS)
            )
            val intent = intents.firstOrNull { it.resolveActivity(packageManager) != null }
            if (intent != null) {
                startActivity(intent)
            }
        }
    }

    private fun updateDefaultLauncherPill() {
        binding.settingsDefaultLauncherPill.text = if (isDefaultLauncher()) {
            getString(R.string.novapulse_default_active)
        } else {
            getString(R.string.novapulse_set_default)
        }
    }

    private fun setupOptionsMenu() {
        binding.settingsToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.about -> launchAbout()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun setupCustomizeColors() {
        binding.settingsColorCustomizationHolder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupUseEnglish() {
        binding.settingsUseEnglishHolder.beVisibleIf(
            beVisible = (config.wasUseEnglishToggled || Locale.getDefault().language != "en")
                    && !isTiramisuPlus()
        )

        binding.settingsUseEnglish.isChecked = config.useEnglish
        binding.settingsUseEnglishHolder.setOnClickListener {
            binding.settingsUseEnglish.toggle()
            config.useEnglish = binding.settingsUseEnglish.isChecked
            exitProcess(0)
        }
    }

    private fun setupDoubleTapToLock() {
        val devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        binding.settingsDoubleTapToLock.isChecked = devicePolicyManager.isAdminActive(
            ComponentName(this, LockDeviceAdminReceiver::class.java)
        )

        binding.settingsDoubleTapToLockHolder.setOnClickListener {
            val isLockDeviceAdminActive = devicePolicyManager.isAdminActive(
                ComponentName(this, LockDeviceAdminReceiver::class.java)
            )
            if (isLockDeviceAdminActive) {
                devicePolicyManager.removeActiveAdmin(
                    ComponentName(this, LockDeviceAdminReceiver::class.java)
                )
            } else {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                intent.putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    ComponentName(this, LockDeviceAdminReceiver::class.java)
                )
                intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.lock_device_admin_hint)
                )
                startActivity(intent)
            }
        }
    }

    private fun setupOpenKeyboardOnAppDrawer() {
        binding.settingsOpenKeyboardOnAppDrawerHolder.beVisibleIf(config.showSearchBar)
        binding.settingsOpenKeyboardOnAppDrawer.isChecked = config.autoShowKeyboardInAppDrawer
        binding.settingsOpenKeyboardOnAppDrawerHolder.setOnClickListener {
            binding.settingsOpenKeyboardOnAppDrawer.toggle()
            config.autoShowKeyboardInAppDrawer = binding.settingsOpenKeyboardOnAppDrawer.isChecked
        }
    }

    private fun setupCloseAppDrawerOnOtherAppOpen() {
        binding.settingsCloseAppDrawerOnOtherApp.isChecked = config.closeAppDrawer
        binding.settingsCloseAppDrawerOnOtherAppHolder.setOnClickListener {
            binding.settingsCloseAppDrawerOnOtherApp.toggle()
            config.closeAppDrawer = binding.settingsCloseAppDrawerOnOtherApp.isChecked
        }
    }

    private fun setupDrawerColumnCount() {
        val currentColumnCount = config.drawerColumnCount
        binding.settingsDrawerColumnCount.text = currentColumnCount.toString()
        binding.settingsDrawerColumnCountHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            for (i in 1..MAX_COLUMN_COUNT) {
                items.add(
                    RadioItem(
                        id = i,
                        title = resources.getQuantityString(
                            org.fossify.commons.R.plurals.column_counts, i, i
                        )
                    )
                )
            }

            RadioGroupDialog(this, items, currentColumnCount) {
                val newColumnCount = it as Int
                if (currentColumnCount != newColumnCount) {
                    config.drawerColumnCount = newColumnCount
                    setupDrawerColumnCount()
                }
            }
        }
    }

    private fun setupDrawerSearchBar() {
        val showSearchBar = config.showSearchBar
        binding.settingsShowSearchBar.isChecked = showSearchBar
        binding.settingsDrawerSearchHolder.setOnClickListener {
            binding.settingsShowSearchBar.toggle()
            config.showSearchBar = binding.settingsShowSearchBar.isChecked
            binding.settingsOpenKeyboardOnAppDrawerHolder.beVisibleIf(config.showSearchBar)
        }
    }

    private fun setupShowDrawerAppLabels() {
        binding.settingsShowDrawerAppLabels.isChecked = config.showDrawerAppLabels
        binding.settingsShowDrawerAppLabelsHolder.setOnClickListener {
            binding.settingsShowDrawerAppLabels.toggle()
            config.showDrawerAppLabels = binding.settingsShowDrawerAppLabels.isChecked
        }
    }

    private fun setupHomeRowCount() {
        val currentRowCount = config.homeRowCount
        binding.settingsHomeScreenRowCount.text = currentRowCount.toString()
        binding.settingsHomeScreenRowCountHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            for (i in MIN_ROW_COUNT..MAX_ROW_COUNT) {
                items.add(
                    RadioItem(
                        id = i,
                        title = resources.getQuantityString(
                            org.fossify.commons.R.plurals.row_counts, i, i
                        )
                    )
                )
            }

            RadioGroupDialog(this, items, currentRowCount) {
                val newRowCount = it as Int
                if (currentRowCount != newRowCount) {
                    config.homeRowCount = newRowCount
                    setupHomeRowCount()
                }
            }
        }
    }

    private fun setupHomeColumnCount() {
        val currentColumnCount = config.homeColumnCount
        binding.settingsHomeScreenColumnCount.text = currentColumnCount.toString()
        binding.settingsHomeScreenColumnCountHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            for (i in MIN_COLUMN_COUNT..MAX_COLUMN_COUNT) {
                items.add(
                    RadioItem(
                        id = i,
                        title = resources.getQuantityString(
                            org.fossify.commons.R.plurals.column_counts, i, i
                        )
                    )
                )
            }

            RadioGroupDialog(this, items, currentColumnCount) {
                val newColumnCount = it as Int
                if (currentColumnCount != newColumnCount) {
                    config.homeColumnCount = newColumnCount
                    setupHomeColumnCount()
                }
            }
        }
    }

    private fun setupShowHomeAppLabels() {
        binding.settingsShowHomeAppLabels.isChecked = config.showHomeAppLabels
        binding.settingsShowHomeAppLabelsHolder.setOnClickListener {
            binding.settingsShowHomeAppLabels.toggle()
            config.showHomeAppLabels = binding.settingsShowHomeAppLabels.isChecked
        }
    }

    @SuppressLint("NewApi")
    private fun setupLanguage() {
        binding.settingsLanguage.text = Locale.getDefault().displayLanguage
        binding.settingsLanguageHolder.beVisibleIf(isTiramisuPlus())
        binding.settingsLanguageHolder.setOnClickListener {
            launchChangeAppLanguageIntent()
        }
    }

    private fun setupManageHiddenIcons() {
        binding.settingsManageHiddenIconsHolder.setOnClickListener {
            startActivity(Intent(this, HiddenIconsActivity::class.java))
        }
    }

    private fun launchAbout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.novapulse_about_title)
            .setMessage(getString(R.string.novapulse_about_body, BuildConfig.VERSION_NAME))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
