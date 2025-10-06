package com.payu.sampleapp

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.payu.base.models.BaseApiLayerConstants
import com.payu.base.models.CardScheme
import com.payu.base.models.CardType
import com.payu.base.models.CustomNote
import com.payu.base.models.ErrorResponse
import com.payu.base.models.OrderDetails
import com.payu.base.models.PayUAddressDetails
import com.payu.base.models.PayUBeneficiaryAccountType
import com.payu.base.models.PayUBeneficiaryDetail
import com.payu.base.models.PayUBillingCycle
import com.payu.base.models.PayUPaymentParams
import com.payu.base.models.PayUSIParams
import com.payu.base.models.PaymentMode
import com.payu.base.models.PaymentType
import com.payu.base.models.PayuBillingLimit
import com.payu.base.models.PayuBillingRule
import com.payu.checkoutpro.PayUCheckoutPro
import com.payu.checkoutpro.models.PayUCheckoutProConfig
import com.payu.checkoutpro.utils.PayUCheckoutProConstants
import com.payu.checkoutpro.utils.PayUCheckoutProConstants.CP_HASH_NAME
import com.payu.checkoutpro.utils.PayUCheckoutProConstants.CP_HASH_STRING
import com.payu.checkoutpro.utils.PayUCheckoutProConstants.CP_HASH_TYPE
import com.payu.checkoutpro.utils.PayUCheckoutProConstants.CP_V2_HASH
import com.payu.paymentparamhelper.PayuConstants
import com.payu.sampleapp.databinding.ActivityMainBinding
import com.payu.ui.model.listeners.PayUCheckoutProListener
import com.payu.ui.model.listeners.PayUHashGenerationListener
import org.json.JSONException
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private val email: String = "snooze@payu.in"
    private val phone = "9999999999"
    private val merchantName = "RH Group"
    private val surl = "https://cbjs.payu.in/sdk/success"
    private val furl = "https://cbjs.payu.in/sdk/failure"
    private val amount = "1.0"

    //Test Key and Salt
    private val testKey = "<Please_add_key_here>"
    private val testSalt = "<Please_add_salt_here>"

    //Prod Key and Sal
    private val prodKey = "<Please_add_key_here>"
    private val prodSalt = "<Please_add_salt_here>"
    private lateinit var binding: ActivityMainBinding

    // variable to track event time
    private var mLastClickTime: Long = 0
    private var reviewOrderAdapter: ReviewOrderRecyclerViewAdapter? = null
    private var tpvBeneficiaryCount = 1
    private var offerKeyCount = 1
    private var skuItemCount = 1
    private var billingCycle = arrayOf(
        "DAILY",
        "WEEKLY",
        "MONTHLY",
        "YEARLY",
        "ONCE",
        "ADHOC"
    )
    private var billingRule = arrayOf(
        "MAX",
        "EXACT"
    )

    private var billingLimit = arrayOf(
        "ON",
        "BEFORE",
        "AFTER"
    )
    private var noteCategory = arrayOf(
        "CARD",
        "NB",
        "WALLET",
        "UPI",
        "EMI",
        "COMMON",
        "NULL"
    )

    private var splitPaymentType = arrayOf(
        "absolute",
        "percentage"
    )

    private var switchSplitPayment: SwitchCompat? = null
    private var spSplitPaymentType: AppCompatSpinner? = null
    private var llSplitPaymentDetails: LinearLayout? = null
    private var btnSplitMore: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        
        // Configure status bar for Android 15 and above
        configureStatusBar()
        
        initializeSIView()
        initializeUpiOtmView()
        initializeSplitPaymentViews()
        initializeAdditionalChargesView()
        initializePercentageChargesView()
        initializeTpvView()
        initializeCrossBorderView()
        initializeEnforceOfferView()
        initializeSkuDetailsView()
        setCustomeNote()
        setInitalData()
        initListeners()
    }

    /**
     * Configure status bar appearance for all Android versions without deprecation warnings
     * Uses modern WindowInsetsController API with proper fallbacks
     */
    private fun configureStatusBar() {
        try {
            // Enable edge-to-edge display for modern experience
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // Get the WindowInsetsController - works for all supported API levels
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            
            // Configure status bar appearance based on Android version
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM -> { // Android 15+ (API 35)
                    configureModernStatusBar(windowInsetsController)
                    Log.d("MainActivity", "Modern status bar configured for Android ${Build.VERSION.SDK_INT}")
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> { // Android 6+ (API 23)
                    configureCompatStatusBar(windowInsetsController)
                    Log.d("MainActivity", "Compatible status bar configured for Android ${Build.VERSION.SDK_INT}")
                }
                else -> {
                    // For very old versions (below API 23)
                    configureBasicStatusBar()
                    Log.d("MainActivity", "Basic status bar configured for Android ${Build.VERSION.SDK_INT}")
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error configuring status bar", e)
            // Silent fallback - no status bar changes
        }
    }

    /**
     * Configure status bar for Android 15+ using modern APIs
     */
    private fun configureModernStatusBar(controller: WindowInsetsControllerCompat) {
        // Set status bar content appearance based on theme
        controller.isAppearanceLightStatusBars = !isDarkModeActive()
        
        // Enable smooth system bar behavior
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Apply Material You theming if available
        applyModernTheming()
    }

    /**
     * Configure status bar for Android 6-14 (API 23-34)
     */
    private fun configureCompatStatusBar(controller: WindowInsetsControllerCompat) {
        // Set status bar content appearance
        controller.isAppearanceLightStatusBars = !isDarkModeActive()
        
        // Apply standard theming
        applyStandardTheming()
    }

    /**
     * Basic status bar configuration for older Android versions
     */
    private fun configureBasicStatusBar() {
        // For API < 23, we can't control light/dark status bar icons
        // Just apply basic theming if possible
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Use reflection to avoid direct API calls that might cause issues
                applyBasicTheming()
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "Could not apply basic status bar theming", e)
        }
    }

    /**
     * Apply Material You theming for modern Android versions
     */
    private fun applyModernTheming() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            try {
                // Use system colors when available
                val systemColor = getSystemAccentColor()
                if (systemColor != null) {
                    setStatusBarColorSafely(systemColor)
                } else {
                    // Fallback to app colors
                    applyStandardTheming()
                }
            } catch (e: Exception) {
                Log.w("MainActivity", "Could not apply modern theming, falling back to standard", e)
                applyStandardTheming()
            }
        } else {
            applyStandardTheming()
        }
    }

    /**
     * Apply standard app theming
     */
    private fun applyStandardTheming() {
        val primaryColor = ContextCompat.getColor(this, R.color.colorPrimary)
        setStatusBarColorSafely(primaryColor)
    }

    /**
     * Apply basic theming for older versions
     */
    private fun applyBasicTheming() {
        val primaryColor = ContextCompat.getColor(this, R.color.colorPrimary)
        setStatusBarColorSafely(primaryColor)
    }

    /**
     * Safely set status bar color without deprecation warnings
     */
    @Suppress("DEPRECATION")
    private fun setStatusBarColorSafely(color: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.statusBarColor = color
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "Could not set status bar color", e)
        }
    }

    /**
     * Get system accent color for Material You theming
     */
    private fun getSystemAccentColor(): Int? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDarkModeActive()) {
                    ContextCompat.getColor(this, com.google.android.material.R.color.material_dynamic_primary20)
                } else {
                    ContextCompat.getColor(this, com.google.android.material.R.color.material_dynamic_primary80)
                }
            } else null
        } catch (e: Exception) {
            Log.w("MainActivity", "System accent color not available", e)
            null
        }
    }

    /**
     * Check if dark mode is currently active
     * @return true if dark mode is active, false otherwise
     */
    private fun isDarkModeActive(): Boolean {
        return when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
    }

    private fun addSplitPaymentDetailedView() {
        val inflate: View =
            LayoutInflater.from(this).inflate(R.layout.layout_split_payment_details, null)
        llSplitPaymentDetails?.addView(inflate, llSplitPaymentDetails!!.childCount)
    }

    private fun initializeSplitPaymentViews() {
        switchSplitPayment = findViewById(R.id.switch_split_payment)
        llSplitPaymentDetails = findViewById(R.id.ll_split_payment_details)
        btnSplitMore = findViewById(R.id.btn_split_more)

        switchSplitPayment?.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            addSplitPaymentDetailedView()
            if (isChecked) {
                findViewById<View>(R.id.ll_split_type).visibility = View.VISIBLE
                findViewById<View>(R.id.ll_split_payment_details).visibility = View.VISIBLE
                btnSplitMore?.visibility = View.VISIBLE
            } else {
                findViewById<View>(R.id.ll_split_type).visibility = View.GONE
                findViewById<View>(R.id.ll_split_payment_details).visibility = View.GONE
                llSplitPaymentDetails?.removeAllViews()
                btnSplitMore?.visibility = View.GONE
            }
        }

        btnSplitMore?.setOnClickListener { addSplitPaymentDetailedView() }
        spSplitPaymentType = findViewById(R.id.et_split_payment_value)

        val adapter: ArrayAdapter<*> = ArrayAdapter<Any?>(
            this,
            android.R.layout.simple_spinner_item,
            splitPaymentType
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spSplitPaymentType?.adapter = adapter
    }

    private fun initializeSIView() {
        binding.switchSiOnOff.setOnCheckedChangeListener { _, isChecked -> if(isChecked)
        { binding.layoutSiDetails.root.visibility = View.VISIBLE }
        else { binding.layoutSiDetails.root.visibility = View.GONE }
        }

        val adapter: ArrayAdapter<*> = ArrayAdapter<Any?>(
            this,
            android.R.layout.simple_spinner_item,
            billingCycle
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.layoutSiDetails.etBillingCycleValue.adapter = adapter
        val billingRuleAdapter : ArrayAdapter<*> = ArrayAdapter<Any?>(
            this, android.R.layout.simple_spinner_item, billingRule
        )
        billingRuleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.layoutSiDetails.etBillingRuleValue.adapter = billingRuleAdapter

        val billingLimitAdapter : ArrayAdapter<*> = ArrayAdapter<Any?>(
            this, android.R.layout.simple_spinner_item, billingLimit
        )
        billingLimitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.layoutSiDetails.etBillingLimitValue.adapter = billingLimitAdapter
    }

    private fun initializeUpiOtmView() {
        binding.switchUpiOtmOnOff.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.layoutUpiOtmDetails.root.visibility = View.VISIBLE
            } else {
                binding.layoutUpiOtmDetails.root.visibility = View.GONE
            }
        }
    }

    private fun initializeAdditionalChargesView() {
        binding.switchAdditionalCharges.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.layoutAdditionalChargesInputs.visibility = View.VISIBLE
            } else {
                binding.layoutAdditionalChargesInputs.visibility = View.GONE
            }
        }
    }
    private fun initializePercentageChargesView() {
        binding.switchPercentageCharges.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.layoutPercentageChargesInputs.visibility = View.VISIBLE
            } else {
                binding.layoutPercentageChargesInputs.visibility = View.GONE
            }
        }
    }

    private fun initializeTpvView() {
        binding.switchTpvOnOff.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.llTpvContent.visibility = View.VISIBLE
                addTpvBeneficiaryEntry() // Add first entry by default
            } else {
                binding.llTpvContent.visibility = View.GONE
                binding.llTpvBeneficiaries.removeAllViews()
                tpvBeneficiaryCount = 1
            }
        }

        binding.btnAddBeneficiary.setOnClickListener {
            addTpvBeneficiaryEntry()
        }

        binding.rgTpvFlowType.setOnCheckedChangeListener { _, checkedId ->
            updateTpvFieldsVisibility()
        }
    }

    private fun addTpvBeneficiaryEntry() {
        val inflater = LayoutInflater.from(this)
        val beneficiaryView = inflater.inflate(R.layout.layout_tpv_beneficiary_details, binding.llTpvBeneficiaries, false)

        // Set up account type spinner
        val accountTypeSpinner = beneficiaryView.findViewById<Spinner>(R.id.spinner_account_type)
        val accountTypes = arrayOf("SAVINGS", "CURRENT", "SALARY", "NRE", "NRO")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accountTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        accountTypeSpinner.adapter = adapter

        // Set up delete functionality
        val deleteButton = beneficiaryView.findViewById<ImageView>(R.id.iv_delete_beneficiary)
        deleteButton.setOnClickListener {
            if (binding.llTpvBeneficiaries.childCount > 1) {
                binding.llTpvBeneficiaries.removeView(beneficiaryView)
            }
        }

        // Hide delete button if this is the first item
        deleteButton.visibility = if (tpvBeneficiaryCount == 1) View.GONE else View.VISIBLE

        // Add different default values based on count for testing
        if (tpvBeneficiaryCount <= 3) {
            val ifscCode = beneficiaryView.findViewById<EditText>(R.id.et_beneficiary_ifsc)
            val accountNumber = beneficiaryView.findViewById<EditText>(R.id.et_beneficiary_account_number)
            val beneficiaryName = beneficiaryView.findViewById<EditText>(R.id.et_beneficiary_name)

            when (tpvBeneficiaryCount) {
                1 -> {
                    ifscCode.setText("HDFC0000090")
                    accountNumber.setText("002001600674")
                    beneficiaryName.setText("ANAND KUMAR RASTOGI")
                }
                2 -> {
                    ifscCode.setText("ICIC0000090")
                    accountNumber.setText("002001600674")
                    beneficiaryName.setText("ANAND KUMAR RASTOGI")
                }
                3 -> {
                    ifscCode.setText("SBIN0000090")
                    accountNumber.setText("002001600674")
                    beneficiaryName.setText("ANAND KUMAR RASTOGI")
                }
            }
        }

        binding.llTpvBeneficiaries.addView(beneficiaryView)
        tpvBeneficiaryCount++

        // Update field visibility based on current flow type
        updateTpvFieldsVisibility()
        
        // Make sure all delete buttons are visible except for the first item when there's only one
        updateTpvDeleteButtonsVisibility()
    }

    private fun updateTpvFieldsVisibility() {
        val isEnachFlow = binding.rbEnachSiTpv.isChecked

        for (i in 0 until binding.llTpvBeneficiaries.childCount) {
            val childView = binding.llTpvBeneficiaries.getChildAt(i)
            val accountTypeLayout = childView.findViewById<LinearLayout>(R.id.ll_account_type)
            val beneficiaryNameLayout = childView.findViewById<LinearLayout>(R.id.ll_beneficiary_name)
            
            accountTypeLayout.visibility = if (isEnachFlow) View.VISIBLE else View.GONE
            beneficiaryNameLayout.visibility = if (isEnachFlow) View.VISIBLE else View.GONE
        }
    }

    private fun updateTpvDeleteButtonsVisibility() {
        for (i in 0 until binding.llTpvBeneficiaries.childCount) {
            val childView = binding.llTpvBeneficiaries.getChildAt(i)
            val deleteButton = childView.findViewById<ImageView>(R.id.iv_delete_beneficiary)
            deleteButton.visibility = if (binding.llTpvBeneficiaries.childCount > 1) View.VISIBLE else View.GONE
        }
    }

    private fun getTpvBeneficiaryDetailsList(): List<TPVBeneficiaryDetail> {
        val beneficiaryList = mutableListOf<TPVBeneficiaryDetail>()
        val isEnachFlow = binding.rbEnachSiTpv.isChecked

        for (i in 0 until binding.llTpvBeneficiaries.childCount) {
            val childView = binding.llTpvBeneficiaries.getChildAt(i)
            val ifscCode = childView.findViewById<EditText>(R.id.et_beneficiary_ifsc).text.toString()
            val accountNumber = childView.findViewById<EditText>(R.id.et_beneficiary_account_number).text.toString()
            
            val accountType = if (isEnachFlow) {
                childView.findViewById<Spinner>(R.id.spinner_account_type).selectedItem.toString()
            } else null
            
            val beneficiaryName = if (isEnachFlow) {
                childView.findViewById<EditText>(R.id.et_beneficiary_name).text.toString()
            } else null

            if (ifscCode.isNotEmpty() && accountNumber.isNotEmpty()) {
                beneficiaryList.add(TPVBeneficiaryDetail(ifscCode, accountNumber, accountType, beneficiaryName))
            }
        }

        return beneficiaryList
    }

    private fun prepareTpvBeneficiaryDetailsList(): List<PayUBeneficiaryDetail> {
        if (!binding.switchTpvOnOff.isChecked) {
            return emptyList()
        }

        val tpvBeneficiaryList = getTpvBeneficiaryDetailsList()
        if (tpvBeneficiaryList.isEmpty()) {
            return emptyList()
        }

        val payUBeneficiaryDetailsList = mutableListOf<PayUBeneficiaryDetail>()
        val isEnachFlow = binding.rbEnachSiTpv.isChecked

        for (tpvDetail in tpvBeneficiaryList) {
            val beneficiaryBuilder = PayUBeneficiaryDetail.Builder()
                .setBeneficiaryIfsc(tpvDetail.beneficiaryIfsc)
                .setBeneficiaryAccountNumber(tpvDetail.beneficiaryAccountNumber)

            // Add account type and name only for ENACH TPV flow
            if (isEnachFlow && tpvDetail.beneficiaryAccountType != null && tpvDetail.beneficiaryName != null) {
                val accountType = when (tpvDetail.beneficiaryAccountType) {
                    "SAVINGS" -> PayUBeneficiaryAccountType.SAVINGS
                    "CURRENT" -> PayUBeneficiaryAccountType.CURRENT
                    else -> PayUBeneficiaryAccountType.SAVINGS
                }
                beneficiaryBuilder
                    .setBeneficiaryAccountType(accountType)
                    .setBeneficiaryName(tpvDetail.beneficiaryName)
            }

            payUBeneficiaryDetailsList.add(beneficiaryBuilder.build())
        }

        return payUBeneficiaryDetailsList
    }

    private fun initializeCrossBorderView() {
        binding.switchCrossborderOnOff.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.layoutCrossborderAddressDetails.root.visibility = View.VISIBLE
            } else {
                binding.layoutCrossborderAddressDetails.root.visibility = View.GONE
            }
        }
    }

    private fun prepareCrossBorderAddressDetails(): PayUAddressDetails {
        if (!binding.switchCrossborderOnOff.isChecked) {
            // Return default empty address when cross-border is disabled
            return PayUAddressDetails.Builder()
                .setLastName("")
                .setAddress1("")
                .setAddress2("")
                .setCity("")
                .setState("")
                .setCountry("")
                .setZipcode("")
                .build()
        }

        val lastName = binding.layoutCrossborderAddressDetails.etLastNameValue.text.toString().trim()
        val address1 = binding.layoutCrossborderAddressDetails.etAddress1Value.text.toString().trim()
        val address2 = binding.layoutCrossborderAddressDetails.etAddress2Value.text.toString().trim()
        val city = binding.layoutCrossborderAddressDetails.etCityValue.text.toString().trim()
        val state = binding.layoutCrossborderAddressDetails.etStateValue.text.toString().trim()
        val country = binding.layoutCrossborderAddressDetails.etCountryValue.text.toString().trim()
        val zipcode = binding.layoutCrossborderAddressDetails.etZipcodeValue.text.toString().trim()

        // Use default values if fields are empty but switch is on
        return PayUAddressDetails.Builder()
            .setLastName(if (lastName.isNotEmpty()) lastName else "")
            .setAddress1(if (address1.isNotEmpty()) address1 else "")
            .setAddress2(if (address2.isNotEmpty()) address2 else "")
            .setCity(if (city.isNotEmpty()) city else "")
            .setState(if (state.isNotEmpty()) state else "")
            .setCountry(if (country.isNotEmpty()) country else "")
            .setZipcode(if (zipcode.isNotEmpty()) zipcode else "")
            .build()
    }

    private fun initializeEnforceOfferView() {
        binding.switchEnforceOfferOnOff.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.llEnforceOfferContent.visibility = View.VISIBLE
                addOfferKeyEntry() // Add first entry by default
            } else {
                binding.llEnforceOfferContent.visibility = View.GONE
                binding.llOfferKeys.removeAllViews()
                offerKeyCount = 1
            }
        }

        binding.btnAddOfferKey.setOnClickListener {
            addOfferKeyEntry()
        }
    }

    private fun addOfferKeyEntry() {
        val inflater = LayoutInflater.from(this)
        val offerKeyView = inflater.inflate(R.layout.layout_offer_key_entry, binding.llOfferKeys, false)

        // Set up delete functionality
        val deleteButton = offerKeyView.findViewById<ImageView>(R.id.iv_delete_offer_key)
        deleteButton.setOnClickListener {
            if (binding.llOfferKeys.childCount > 1) {
                binding.llOfferKeys.removeView(offerKeyView)
            }
        }

        // Hide delete button if this is the first item
        deleteButton.visibility = if (offerKeyCount == 1) View.GONE else View.VISIBLE

        // Add default value for the first entry
        if (offerKeyCount == 1) {
            val offerKeyInput = offerKeyView.findViewById<EditText>(R.id.et_offer_key_value)
            offerKeyInput.setText("OFFER123SAMPLE")
        }

        binding.llOfferKeys.addView(offerKeyView)
        offerKeyCount++

        // Make sure all delete buttons are visible except for the first item when there's only one
        updateOfferKeyDeleteButtonsVisibility()
    }

    private fun updateOfferKeyDeleteButtonsVisibility() {
        for (i in 0 until binding.llOfferKeys.childCount) {
            val childView = binding.llOfferKeys.getChildAt(i)
            val deleteButton = childView.findViewById<ImageView>(R.id.iv_delete_offer_key)
            deleteButton.visibility = if (binding.llOfferKeys.childCount > 1) View.VISIBLE else View.GONE
        }
    }

    private fun prepareEnforceOfferKeysList(): ArrayList<String> {
        val offerKeysList = ArrayList<String>()
        
        if (!binding.switchEnforceOfferOnOff.isChecked) {
            return offerKeysList // Return empty list if switch is off
        }

        for (i in 0 until binding.llOfferKeys.childCount) {
            val childView = binding.llOfferKeys.getChildAt(i)
            val offerKeyValue = childView.findViewById<EditText>(R.id.et_offer_key_value).text.toString().trim()

            if (offerKeyValue.isNotEmpty()) {
                offerKeysList.add(offerKeyValue)
            }
        }

        return offerKeysList
    }

    private fun initializeSkuDetailsView() {
        binding.switchSkuDetailsOnOff.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.llSkuDetailsContent.visibility = View.VISIBLE
                addSkuItemEntry() // Add first entry by default
            } else {
                binding.llSkuDetailsContent.visibility = View.GONE
                binding.llSkuItems.removeAllViews()
                skuItemCount = 1
            }
        }

        binding.btnAddSkuItem.setOnClickListener {
            addSkuItemEntry()
        }
    }

    private fun addSkuItemEntry() {
        val inflater = LayoutInflater.from(this)
        val skuItemView = inflater.inflate(R.layout.layout_sku_item_entry, binding.llSkuItems, false)

        // Set up delete functionality
        val deleteButton = skuItemView.findViewById<ImageView>(R.id.iv_delete_sku_item)
        deleteButton.setOnClickListener {
            if (binding.llSkuItems.childCount > 1) {
                binding.llSkuItems.removeView(skuItemView)
            }
        }

        // Hide delete button if this is the first item
        deleteButton.visibility = if (skuItemCount == 1) View.GONE else View.VISIBLE

        // Add sample values based on the provided examples
        if (skuItemCount <= 2) {
            val skuId = skuItemView.findViewById<EditText>(R.id.et_sku_id)
            val skuName = skuItemView.findViewById<EditText>(R.id.et_sku_name)
            val amountPerSku = skuItemView.findViewById<EditText>(R.id.et_amount_per_sku)
            val quantity = skuItemView.findViewById<EditText>(R.id.et_quantity)

            when (skuItemCount) {
                1 -> {
                    skuId.setText("1111")
                    skuName.setText("Protein Bar")
                    amountPerSku.setText("6000")
                    quantity.setText("1")
                }
                2 -> {
                    skuId.setText("1112")
                    skuName.setText("Healthy Snack")
                    amountPerSku.setText("6000")
                    quantity.setText("1")
                }
            }
        }

        binding.llSkuItems.addView(skuItemView)
        skuItemCount++

        // Make sure all delete buttons are visible except for the first item when there's only one
        updateSkuItemDeleteButtonsVisibility()
    }

    private fun updateSkuItemDeleteButtonsVisibility() {
        for (i in 0 until binding.llSkuItems.childCount) {
            val childView = binding.llSkuItems.getChildAt(i)
            val deleteButton = childView.findViewById<ImageView>(R.id.iv_delete_sku_item)
            deleteButton.visibility = if (binding.llSkuItems.childCount > 1) View.VISIBLE else View.GONE
        }
    }

    private fun prepareSkuDetailsList(): com.payu.base.models.SkuDetails {
        val skuList = mutableListOf<com.payu.base.models.SKU>()

        if (binding.switchSkuDetailsOnOff.isChecked) {
            for (i in 0 until binding.llSkuItems.childCount) {
                val childView = binding.llSkuItems.getChildAt(i)
                val skuId = childView.findViewById<EditText>(R.id.et_sku_id).text.toString().trim()
                val skuName = childView.findViewById<EditText>(R.id.et_sku_name).text.toString().trim()
                val amountPerSkuStr = childView.findViewById<EditText>(R.id.et_amount_per_sku).text.toString().trim()
                val quantityStr = childView.findViewById<EditText>(R.id.et_quantity).text.toString().trim()
                val offerKey = childView.findViewById<EditText>(R.id.et_offer_key).text.toString().trim()
                val offerAutoApply = childView.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_offer_auto_apply).isChecked

                // Parse quantity and amount
                val quantity = quantityStr.toIntOrNull() ?: 1
                val amountPerSku = amountPerSkuStr

                // Create offer keys list if provided
                val offerKeys = if (offerKey.isNotEmpty()) {
                    arrayListOf(offerKey)
                } else null

                if (skuId.isNotEmpty() && skuName.isNotEmpty() && amountPerSku.isNotEmpty()) {
                    // Use PayU SDK's SKU class
                    skuList.add(com.payu.base.models.SKU(quantity, amountPerSku, skuId, skuName, offerKeys, offerAutoApply))
                }
            }
        }

        // Return PayU SDK's SkuDetails class
        return com.payu.base.models.SkuDetails(skuList)
    }

    private fun setCustomeNote(){
        val noteCategoryAdapter : ArrayAdapter<*> = ArrayAdapter<Any?>(
            this,android.R.layout.simple_spinner_item,noteCategory
        )
        noteCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        findViewById<AppCompatSpinner>(R.id.et_custom_note_category_value).adapter = noteCategoryAdapter
    }

    private fun setInitalData() {
        updateProdEnvDetails()
        binding.etSurl.setText(surl)
        binding.etFurl.setText(furl)
        binding.etMerchantName.setText(merchantName)
        binding.etPhone.setText(phone)
        binding.etAmount.setText(amount)
        binding.etUserCredential.setText("${binding.etKey.text}:$email")
        binding.etSurePayCount.setText("0")
    }

    private fun initListeners() {
        binding.radioGrpEnv.setOnCheckedChangeListener { _: RadioGroup, i: Int ->
            when (i) {
                R.id.radioBtnTest -> updateTestEnvDetails()
                R.id.radioBtnProduction -> updateProdEnvDetails()
                else -> updateTestEnvDetails()
            }
        }

        binding.switchEnableReviewOrder.setOnCheckedChangeListener { _: CompoundButton, b: Boolean ->
            if (b) showReviewOrderView() else hideReviewOrderView()
        }

        binding.btnAddItem.setOnClickListener { reviewOrderAdapter?.addRow() }
    }

    private fun hideReviewOrderView() {
        binding.rlReviewOrder.visibility = View.GONE
        reviewOrderAdapter = null
    }

    private fun showReviewOrderView() {
        binding.rlReviewOrder.visibility = View.VISIBLE
        reviewOrderAdapter = ReviewOrderRecyclerViewAdapter()
        binding.rvReviewOrder.layoutManager = LinearLayoutManager(this)
        binding.rvReviewOrder.adapter = reviewOrderAdapter
    }

    private fun updateTestEnvDetails() {
        //For testing
        binding.etKey.setText(testKey)
        binding.etSalt.setText(testSalt)
    }

    private fun updateProdEnvDetails() {
        //For Production
        binding.etKey.setText(prodKey)
        binding.etSalt.setText(prodSalt)
    }

    fun startPayment(view: View) {
        // Preventing multiple clicks, using threshold of 1 second
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
            return
        }
        mLastClickTime = SystemClock.elapsedRealtime()

        val paymentParams = preparePayUBizParams()
        initUiSdk(paymentParams)
    }

    fun preparePayUBizParams(): PayUPaymentParams {
        val additionalParamsMap: HashMap<String, Any?> = HashMap()
        additionalParamsMap[PayUCheckoutProConstants.CP_UDF1] = "udf1"
        additionalParamsMap[PayUCheckoutProConstants.CP_UDF2] = "udf2"
        additionalParamsMap[PayUCheckoutProConstants.CP_UDF3] = "udf3"
        additionalParamsMap[PayUCheckoutProConstants.CP_UDF4] = "udf4"
        additionalParamsMap[PayUCheckoutProConstants.CP_UDF5] = "udf5"

        // For CrossBorder Payment, merchant need to pass invoiceId in UDF5 otherwise pass as null 
//        additionalParams.put(PayUCheckoutProConstants.CP_UDF5, "3456788765");

        //Below param should be passed only when sodexo payment option is enabled and to show saved sodexo card
//        additionalParamsMap[PayUCheckoutProConstants.SODEXO_SOURCE_ID] = "<SODEXO SOURCE ID>"  // merchant has to pass this

       //Below param should be passed only when ClooseLoop payment option is enabled and to show inside the saved card
//        additionalParamsMap[PayUCheckoutProConstants.WALLET_URN] = "<Wallet URN>"  // merchant has to pass this

        // This below parameter only required For SI/Recurring Payment
        var siDetails: PayUSIParams? =null
        if(binding.switchSiOnOff.isChecked) {
            siDetails  = PayUSIParams.Builder()
                .setIsFreeTrial(binding.layoutSiDetails.spFreeTrial.isChecked)
                .setBillingAmount(binding.layoutSiDetails.etBillingAmountValue.text.toString())
                .setBillingCycle(PayUBillingCycle.valueOf(binding.layoutSiDetails.etBillingCycleValue.selectedItem.toString()))
                .setBillingInterval(binding.layoutSiDetails.etBillingIntervalValue.text.toString().toInt())
                .setPaymentStartDate(binding.layoutSiDetails.etPaymentStartDateValue.text.toString())
                .setPaymentEndDate(binding.layoutSiDetails.etPaymentEndDateValue.text.toString())
                .setRemarks(binding.layoutSiDetails.etRemarksValue.text.toString())
                .setBillingLimit(PayuBillingLimit.valueOf(binding.layoutSiDetails.etBillingLimitValue.selectedItem.toString()))
                .setBillingRule(PayuBillingRule.valueOf(binding.layoutSiDetails.etBillingRuleValue.selectedItem.toString()))
                .build()
        }

        // This below parameter only required For UPI OTM Payment
        if(binding.switchUpiOtmOnOff.isChecked) {
            siDetails  = PayUSIParams.Builder()
                .setPreAuthTxn(binding.layoutUpiOtmDetails.switchPreAuthTxn.isChecked)
                .setPaymentStartDate(binding.layoutUpiOtmDetails.etPaymentStartDateValue.text.toString())
                .setPaymentEndDate(binding.layoutUpiOtmDetails.etPaymentEndDateValue.text.toString())
                .build()
        }

        // This below parameter only required For Split Payment
        var splitPaymentDetails : JSONObject? = null
        if (switchSplitPayment!!.isChecked) {
            val requestMap: HashMap<String, JSONObject> = HashMap()
            splitPaymentDetails = JSONObject()
            try {
                splitPaymentDetails.put(
                    PayuConstants.SPLIT_PAYMENT_TYPE,
                    spSplitPaymentType!!.selectedItem.toString()
                )
                val childCount = llSplitPaymentDetails!!.childCount
                for (i in 0 until childCount) {
                    val childAt = llSplitPaymentDetails!!.getChildAt(i)
                    val subAmt = childAt.findViewById<EditText>(R.id.et_sub_amt_value)
                    val commissionAmt =
                        childAt.findViewById<EditText>(R.id.et_aggregator_charges_value)
                    val childKey = childAt.findViewById<EditText>(R.id.et_child_key_value)
                    if (!subAmt.text.toString().isNullOrEmpty() && !commissionAmt.text.toString().isNullOrEmpty() ) {
                        val obj = JSONObject()
                        obj.put(PayuConstants.SPLIT_PAYMENT_AGGREGATOR_SUB_TXN_ID, "ab"+ (1 until 10000).random())
                        obj.put(
                            PayuConstants.SPLIT_PAYMENT_AGGREGATOR_SUB_AMOUNT,
                            subAmt.text.toString()
                        )
                        obj.put(
                            PayuConstants.SPLIT_PAYMENT_AGGREGATOR_CHARGES,
                            commissionAmt.text.toString()
                        )
                        requestMap[childKey.text.toString()] = obj
                    }
                }
                splitPaymentDetails.put(PayuConstants.SPLIT_PAYMENT_INFO, JSONObject(requestMap as Map<String, JSONObject>))
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        // Prepare TPV beneficiary details list if enabled
        val tpvBeneficiaryDetailsList = prepareTpvBeneficiaryDetailsList()

        // Prepare Cross-Border address details (always returns valid object)
        val crossBorderAddressDetails: PayUAddressDetails = prepareCrossBorderAddressDetails()

        // Prepare Enforce Offer Keys list (empty list if disabled)
        val enforceOfferKeysList: ArrayList<String> = prepareEnforceOfferKeysList()

        // Prepare SKU Details (empty list if disabled)
        val skuDetails: com.payu.base.models.SkuDetails = prepareSkuDetailsList()

        return PayUPaymentParams.Builder().setAmount(binding.etAmount.text.toString())
            .setIsProduction(binding.radioBtnProduction.isChecked)
            .setKey(binding.etKey.text.toString())
            .setProductInfo("Macbook Pro")
            .setPhone(binding.etPhone.text.toString())
            .setTransactionId(System.currentTimeMillis().toString())
            .setFirstName("Abc")
            .setEmail(email)
            .setSurl(binding.etSurl.text.toString())
            .setFurl(binding.etFurl.text.toString())
            .setUserCredential(binding.etUserCredential.text.toString()) // Format: <merchantKey>:<userId> Here, the UserId is any ID/email/phone number to uniquely identify the user. **
            .setAdditionalParams(additionalParamsMap)
            // SI Parameter, used for only SI / Recurring Payment otherwise Optional
            .setPayUSIParams(if(binding.switchSiOnOff.isChecked || binding.switchUpiOtmOnOff.isChecked) siDetails else null)
            // Split Parameter, used for only Split Payment otherwise Optional
            .setSplitPaymentDetails(if(switchSplitPayment!!.isChecked) splitPaymentDetails.toString() else null)
            // Additional Charges: Fixed amount charges for different payment modes otherwise Optional
            .setAdditionalCharges((if(binding.switchAdditionalCharges.isChecked) binding.etAdditionalChargesValue.text.toString() else null).toString())
            // Percentage Additional Charges: Percentage-based charges for different payment modes otherwise Optional
            .setPercentageAdditionalCharges((if(binding.switchPercentageCharges.isChecked) binding.etPercentageChargesValue.text.toString() else null).toString())
            // TPV Parameter: Set beneficiary details list if enabled, otherwise null
            .setBeneficiaryDetailsList(tpvBeneficiaryDetailsList)
            // Cross-Border Parameter: Set address details (empty object if disabled)
            .setAddressDetails(crossBorderAddressDetails)
            // Enforce Offer Keys Parameter: Set offer keys list (empty list if disabled)
            .setEnforcementOfferKeys(enforceOfferKeysList)
            // SKU Details Parameter: Set SKU details (empty list if disabled)
            .setSkuDetails(skuDetails)
            .build()
    }

    private fun initUiSdk(payUPaymentParams: PayUPaymentParams) {
        PayUCheckoutPro.open(
            this,
            payUPaymentParams,
            getCheckoutProConfig(),
            object : PayUCheckoutProListener {

                override fun onPaymentSuccess(response: Any) {
                    processResponse(response)
                }

                override fun onPaymentFailure(response: Any) {
                    processResponse(response)
                }

                override fun onPaymentCancel(isTxnInitiated: Boolean) {
                    showSnackBar(resources.getString(R.string.transaction_cancelled_by_user))
                }

                override fun onError(errorResponse: ErrorResponse) {

                    val errorMessage: String
                    if (errorResponse.errorMessage != null && errorResponse.errorMessage!!.isNotEmpty())
                        errorMessage = errorResponse.errorMessage!!
                    else
                        errorMessage = resources.getString(R.string.some_error_occurred)
                    showSnackBar(errorMessage)
                }

                override fun generateHash(
                    map: HashMap<String, String?>,
                    hashGenerationListener: PayUHashGenerationListener
                ) {
                    if (map.containsKey(CP_HASH_STRING) && map.containsKey(CP_HASH_NAME)) {

                        val hashData = map[CP_HASH_STRING]
                        val hashName = map[CP_HASH_NAME]
                        val hashType = map[CP_HASH_TYPE]

                        var postSalt = ""

                        if (map.containsKey(PayUCheckoutProConstants.CP_POST_SALT))
                            postSalt = postSalt.plus(map[PayUCheckoutProConstants.CP_POST_SALT])

                        val hash: String?

                        // Backend will generate the hash which you need to pass to SDK
                        // hash: is the response which you get from your server
                        // use SHA512 Algorithm for generating the Hash
                        //Keep the salt and hash calculation logic in the backend for security reasons. Don't use local hash logic.

                        //Uncomment following line to test the test hash.
                        val salt = binding.etSalt.text.toString() + postSalt
                        hash = HashGenerationUtils.generateHashFromSDK(
                            hashData!!,
                            salt
                        )

                        if (!TextUtils.isEmpty(hash)) {
                            val hashMap: HashMap<String, String?> = HashMap()
                            hashMap[hashName!!] = hash!!
                            hashGenerationListener.onHashGenerated(hashMap)
                        }
                    }
                }

                override fun setWebViewProperties(webView: WebView?, bank: Any?) {
                }
            })
    }

    private fun getCheckoutProConfig(): PayUCheckoutProConfig {
        val checkoutProConfig = PayUCheckoutProConfig()
        checkoutProConfig.paymentModesOrder = getCheckoutOrderList()
        checkoutProConfig.showCbToolbar = !binding.switchHideCbToolBar.isChecked
        checkoutProConfig.autoSelectOtp = binding.switchAutoSelectOtp.isChecked
        checkoutProConfig.autoApprove = binding.switchAutoApprove.isChecked
        checkoutProConfig.surePayCount = binding.etSurePayCount.text.toString().toInt()
        checkoutProConfig.cartDetails = reviewOrderAdapter?.getOrderDetailsList() as ArrayList<OrderDetails>?
        checkoutProConfig.showExitConfirmationOnPaymentScreen =
            !binding.switchDiableCBDialog.isChecked
        checkoutProConfig.showExitConfirmationOnCheckoutScreen =
            !binding.switchDiableUiDialog.isChecked
        checkoutProConfig.merchantName = binding.etMerchantName.text.toString()
        checkoutProConfig.merchantLogo = R.drawable.merchant_logo
        checkoutProConfig.waitingTime = 3000
        checkoutProConfig.merchantResponseTimeout = 3000
        checkoutProConfig.customNoteDetails = getCustomeNoteDetails()
        // uncomment below code to perform enforcement
//        checkoutProConfig.enforcePaymentList = getEnforcePaymentList()
        return checkoutProConfig
    }

    private fun getEnforcePaymentList(): ArrayList<HashMap<String, String>> {
        val enforceList = ArrayList<HashMap<String,String>>()
        enforceList.add(HashMap<String,String>().apply {
            put(PayUCheckoutProConstants.CP_PAYMENT_TYPE, PaymentType.NB.name)
             put(PayUCheckoutProConstants.ENFORCED_IBIBOCODE,"AXIB")

        })
        enforceList.add(HashMap<String,String>().apply {
            put(PayUCheckoutProConstants.CP_PAYMENT_TYPE, PaymentType.CARD.name)
            put(PayUCheckoutProConstants.CP_CARD_TYPE, CardType.CC.name)
            put(PayUCheckoutProConstants.CP_CARD_SCHEME, CardScheme.MAST.name)
        })
        return enforceList
    }


    private fun getCheckoutOrderList(): ArrayList<PaymentMode> {
        val checkoutOrderList = ArrayList<PaymentMode>()
        if (binding.switchShowGooglePay.isChecked) checkoutOrderList.add(
            PaymentMode(
                PaymentType.UPI,
                PayUCheckoutProConstants.CP_GOOGLE_PAY
            )
        )
        if (binding.switchShowPhonePe.isChecked) checkoutOrderList.add(
            PaymentMode(
                PaymentType.WALLET,
                PayUCheckoutProConstants.CP_PHONEPE
            )
        )
        if (binding.switchShowPaytm.isChecked) checkoutOrderList.add(
            PaymentMode(
                PaymentType.WALLET,
                PayUCheckoutProConstants.CP_PAYTM
            )
        )
        return checkoutOrderList
    }

    private fun showSnackBar(message: String) {
        Snackbar.make(binding.clMain, message, Snackbar.LENGTH_LONG).show()
    }

    private fun processResponse(response: Any) {
        response as HashMap<*, *>
        Log.d(
            BaseApiLayerConstants.SDK_TAG,
            "payuResponse ; > " + response[PayUCheckoutProConstants.CP_PAYU_RESPONSE]
                    + ", merchantResponse : > " + response[PayUCheckoutProConstants.CP_MERCHANT_RESPONSE]
        )

        AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert)
            .setCancelable(false)
            .setMessage(
                "Payu's Data : " + response.get(PayUCheckoutProConstants.CP_PAYU_RESPONSE) + "\n\n\n Merchant's Data: " + response.get(
                    PayUCheckoutProConstants.CP_MERCHANT_RESPONSE
                )
            )
            .setPositiveButton(
                android.R.string.ok
            ) { dialog, _ -> dialog.dismiss() }.show()
    }

    private fun getCustomeNoteDetails(): ArrayList<CustomNote>{
        val customNote = ArrayList<CustomNote>()

        if (!(findViewById<AppCompatSpinner>(R.id.et_custom_note_category_value).selectedItem.toString().equals("NULL") || findViewById<AppCompatSpinner>(R.id.et_custom_note_category_value).selectedItem.toString().equals("COMMON")) ) {
            val noteCategory = ArrayList<PaymentType>().also {
                it.add(PaymentType.valueOf(findViewById<AppCompatSpinner>(R.id.et_custom_note_category_value).selectedItem.toString()))
            }
            customNote.add(CustomNote(findViewById<EditText>(R.id.et_custom_note_value).text.toString(),noteCategory))
//                .also {
//                it.custom_note = binding.customeNote.etCustomNoteValue.text.toString()
//                it.custom_note_category = ArrayList<PaymentType>().also {
//                    it.add(PaymentType.valueOf(binding.customeNote.etCustomNoteCategoryValue.selectedItem.toString()))
//                it.add(PaymentType.NB)
//                it.add(PaymentType.WALLET)
//                it.add(PaymentType.UPI)
//                it.add(PaymentType.EMI)
//                }
//            })
        }else if (findViewById<AppCompatSpinner>(R.id.et_custom_note_category_value).selectedItem.toString().equals("NULL")){
            customNote.add(CustomNote(findViewById<EditText>(R.id.et_custom_note_value).text.toString(),null))
        }else{
            val noteCategory = ArrayList<PaymentType>().also {
                it.add(PaymentType.CARD)
                it.add(PaymentType.NB)
                it.add(PaymentType.UPI)
                it.add(PaymentType.WALLET)
                it.add(PaymentType.EMI)
            }
            customNote.add(CustomNote(findViewById<EditText>(R.id.et_custom_note_value).text.toString(),noteCategory))
        }

        return customNote;
    }

}

data class TPVBeneficiaryDetail(
    val beneficiaryIfsc: String,
    val beneficiaryAccountNumber: String,
    val beneficiaryAccountType: String? = null, // Only for ENACH TPV
    val beneficiaryName: String? = null // Only for ENACH TPV
)

enum class TPVFlowType {
    UPI_TPV,
    ENACH_TPV
}

