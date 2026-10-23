package com.example.fixflow.ui.newjob

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.fixflow.R
import com.example.fixflow.databinding.FragmentNewRepairJobBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NewRepairJobFragment : Fragment() {

    private var _binding: FragmentNewRepairJobBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewJobViewModel by viewModels()
    private var selectedPhotoUri: Uri? = null
    private var selectedDeadlineCalendar: Calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 2)
    }

    private val pickPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
            binding.ivPhotoPreview.setImageURI(uri)
            binding.ivPhotoPreview.visibility = View.VISIBLE
            binding.btnRemovePhoto.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewRepairJobBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDropdowns()
        setupDatePicker()
        observeViewModel()
        setupListeners()
        viewModel.fetchNextJobId()
    }

    private fun setupDropdowns() {
        val deviceTypes = arrayOf("Smartphone", "Tablet", "Laptop", "Desktop", "Smartwatch", "Audio Device", "Other")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, deviceTypes)
        binding.actvDeviceType.setAdapter(typeAdapter)
        binding.actvDeviceType.setText(deviceTypes[0], false)

        binding.actvDeviceType.setOnClickListener {
            binding.actvDeviceType.showDropDown()
        }
        binding.actvDeviceType.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.actvDeviceType.showDropDown()
        }

        updateBrandSuggestions(deviceTypes[0])

        binding.actvDeviceType.setOnItemClickListener { _, _, position, _ ->
            val selectedType = deviceTypes.getOrNull(position) ?: "Smartphone"
            updateBrandSuggestions(selectedType)
        }

        binding.actvDeviceBrand.setOnClickListener {
            binding.actvDeviceBrand.showDropDown()
        }
        binding.actvDeviceBrand.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.actvDeviceBrand.showDropDown()
        }

        binding.actvDeviceBrand.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentType = binding.actvDeviceType.text.toString().trim()
                val currentBrand = s?.toString()?.trim().orEmpty()
                updateModelSuggestions(currentType, currentBrand)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.actvDeviceBrand.setOnItemClickListener { _, _, _, _ ->
            val currentType = binding.actvDeviceType.text.toString().trim()
            val currentBrand = binding.actvDeviceBrand.text.toString().trim()
            updateModelSuggestions(currentType, currentBrand)
        }

        binding.actvDeviceModel.setOnClickListener {
            binding.actvDeviceModel.showDropDown()
        }
        binding.actvDeviceModel.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.actvDeviceModel.showDropDown()
        }
    }

    private fun updateBrandSuggestions(deviceType: String) {
        val brands = when (deviceType) {
            "Smartphone" -> arrayOf("Apple", "Samsung", "Xiaomi", "OPPO", "vivo", "Realme", "Huawei", "Motorola", "Google", "OnePlus", "Sony", "Infinix", "Tecno", "ASUS", "Lenovo", "Nokia", "Nothing", "Honor", "ZTE", "TCL")
            "Tablet" -> arrayOf("Apple (iPad)", "Samsung (Galaxy Tab)", "Xiaomi (Pad)", "Lenovo", "Huawei", "Realme", "Amazon (Fire)", "Microsoft (Surface)", "ASUS", "Nokia")
            "Laptop" -> arrayOf("ASUS", "Acer", "Dell", "HP", "Lenovo", "Apple (MacBook)", "MSI", "Razer", "Gigabyte", "Toshiba", "Sony", "Samsung", "Microsoft")
            "Desktop" -> arrayOf("Custom System Unit", "ASUS", "Gigabyte", "MSI", "Dell / Alienware", "HP / OMEN", "Lenovo / Legion", "Acer / Predator", "Apple (Mac / iMac)", "Corsair", "NZXT", "Intel", "AMD")
            "Smartwatch" -> arrayOf("Apple", "Samsung", "Xiaomi", "Huawei", "Garmin", "Fitbit", "Amazfit", "Fossil", "Realme", "OPPO")
            "Audio Device" -> arrayOf("Sony", "Bose", "JBL", "Apple (AirPods)", "Sennheiser", "Audio-Technica", "Marshall", "Anker Soundcore", "Beats", "Logitech")
            else -> arrayOf("Generic / Universal")
        }

        val brandAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, brands)
        binding.actvDeviceBrand.setAdapter(brandAdapter)

        if (deviceType == "Desktop") {
            binding.tilDeviceModel.hint = "System Unit Specs / Model *"
        } else {
            binding.tilDeviceModel.hint = getString(R.string.hint_device_model)
        }

        val currentBrand = binding.actvDeviceBrand.text.toString().trim()
        updateModelSuggestions(deviceType, currentBrand)
    }

    private fun updateModelSuggestions(deviceType: String, brand: String) {
        val cleanBrand = brand.lowercase(Locale.getDefault())

        val models = when {
            deviceType == "Smartphone" && cleanBrand.contains("apple") -> arrayOf("iPhone 15 Pro Max", "iPhone 15 Pro", "iPhone 15", "iPhone 14 Pro Max", "iPhone 14", "iPhone 13", "iPhone 12", "iPhone 11", "iPhone SE (2022)")
            deviceType == "Smartphone" && cleanBrand.contains("samsung") -> arrayOf("Galaxy S24 Ultra", "Galaxy S24+", "Galaxy S24", "Galaxy S23 Ultra", "Galaxy A55 5G", "Galaxy A35 5G", "Galaxy A15", "Galaxy Z Fold 5", "Galaxy Z Flip 5")
            deviceType == "Smartphone" && cleanBrand.contains("xiaomi") -> arrayOf("Redmi Note 13 Pro+", "Redmi Note 13", "Xiaomi 14 Ultra", "Xiaomi 13T", "Poco X6 Pro", "Poco F5", "Redmi 13C")
            deviceType == "Smartphone" && cleanBrand.contains("oppo") -> arrayOf("Reno 11 Pro 5G", "Reno 10", "Find N3 Flip", "A98 5G", "A78 5G", "A58", "A38")
            deviceType == "Smartphone" && cleanBrand.contains("vivo") -> arrayOf("V30 Pro 5G", "V29 5G", "Y36", "Y27 5G", "X100 Pro", "Y17s")
            deviceType == "Smartphone" && cleanBrand.contains("realme") -> arrayOf("Realme 12 Pro+ 5G", "Realme 11", "Realme C67", "Realme C55", "Realme GT 5G")
            deviceType == "Smartphone" && cleanBrand.contains("google") -> arrayOf("Pixel 8 Pro", "Pixel 8", "Pixel 7a", "Pixel 7 Pro", "Pixel 6a")

            deviceType == "Laptop" && cleanBrand.contains("asus") -> arrayOf("ROG Strix G16", "TUF Gaming F15", "ZenBook 14 OLED", "VivoBook 15", "ROG Zephyrus G14", "ProArt Studiobook")
            deviceType == "Laptop" && cleanBrand.contains("acer") -> arrayOf("Nitro 5", "Swift 3", "Aspire 5", "Predator Helios 300", "Spin 5")
            deviceType == "Laptop" && cleanBrand.contains("dell") -> arrayOf("XPS 15", "Latitude 5420", "Inspiron 15", "G15 Gaming", "Alienware m16", "Vostro 3510")
            deviceType == "Laptop" && cleanBrand.contains("hp") -> arrayOf("Pavilion 15", "OMEN 16", "Victus 15", "Spectre x360", "Envy 13", "ProBook 450 G8")
            deviceType == "Laptop" && cleanBrand.contains("lenovo") -> arrayOf("Legion 5 Pro", "IdeaPad Slim 3", "ThinkPad X1 Carbon", "Yoga 7i", "LOQ 15")
            deviceType == "Laptop" && cleanBrand.contains("apple") -> arrayOf("MacBook Pro 16\" (M3)", "MacBook Pro 14\" (M3)", "MacBook Air 15\" (M2/M3)", "MacBook Air 13\" (M2/M3)")

            deviceType == "Tablet" && cleanBrand.contains("apple") -> arrayOf("iPad Pro 12.9\" (M2)", "iPad Pro 11\" (M2)", "iPad Air 5th Gen", "iPad 10th Gen", "iPad mini 6th Gen")
            deviceType == "Tablet" && cleanBrand.contains("samsung") -> arrayOf("Galaxy Tab S9 Ultra", "Galaxy Tab S9 FE", "Galaxy Tab A9+", "Galaxy Tab S8")

            deviceType == "Desktop" -> arrayOf("Custom Gaming PC (Ryzen / Core i7)", "Home / Office System Unit", "Mac Studio (M2)", "iMac 24\" (M3)", "Mac mini (M2)", "All-in-One PC Workstation")
            deviceType == "Smartwatch" && cleanBrand.contains("apple") -> arrayOf("Apple Watch Ultra 2", "Apple Watch Series 9", "Apple Watch SE (2023)")
            deviceType == "Smartwatch" && cleanBrand.contains("samsung") -> arrayOf("Galaxy Watch 6 Classic", "Galaxy Watch 6", "Galaxy Watch 5 Pro")

            else -> arrayOf("Series / Model 1", "Series / Model 2", "Standard Edition")
        }

        val modelAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, models)
        binding.actvDeviceModel.setAdapter(modelAdapter)
    }

    private fun setupDatePicker() {
        updateDeadlineDisplay()

        binding.etCompletionDeadline.setOnClickListener {
            val context = requireContext()
            val year = selectedDeadlineCalendar.get(Calendar.YEAR)
            val month = selectedDeadlineCalendar.get(Calendar.MONTH)
            val day = selectedDeadlineCalendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(context, { _, selectedYear, selectedMonth, selectedDay ->
                selectedDeadlineCalendar.set(Calendar.YEAR, selectedYear)
                selectedDeadlineCalendar.set(Calendar.MONTH, selectedMonth)
                selectedDeadlineCalendar.set(Calendar.DAY_OF_MONTH, selectedDay)

                val hour = selectedDeadlineCalendar.get(Calendar.HOUR_OF_DAY)
                val minute = selectedDeadlineCalendar.get(Calendar.MINUTE)

                TimePickerDialog(context, { _, selectedHour, selectedMinute ->
                    selectedDeadlineCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                    selectedDeadlineCalendar.set(Calendar.MINUTE, selectedMinute)
                    updateDeadlineDisplay()
                }, hour, minute, false).show()

            }, year, month, day).show()
        }
    }

    private fun updateDeadlineDisplay() {
        val format = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
        binding.etCompletionDeadline.setText(format.format(selectedDeadlineCalendar.time))
    }

    private fun observeViewModel() {
        viewModel.generatedJobId.observe(viewLifecycleOwner) { jobId ->
            binding.tvGeneratedJobId.text = jobId
        }

        viewModel.saveSuccessEvent.observe(viewLifecycleOwner) { jobId ->
            if (jobId != null) {
                Toast.makeText(requireContext(), "Repair job $jobId registered successfully!", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveEvent()

                val bundle = Bundle().apply {
                    putString("jobId", jobId)
                }
                findNavController().navigate(R.id.nav_repair_detail, bundle)
            }
        }
    }

    private fun setupListeners() {
        binding.btnAttachPhoto.setOnClickListener {
            pickPhotoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.btnRemovePhoto.setOnClickListener {
            selectedPhotoUri = null
            binding.ivPhotoPreview.setImageURI(null)
            binding.ivPhotoPreview.visibility = View.GONE
            binding.btnRemovePhoto.visibility = View.GONE
        }

        binding.btnSaveRepairJob.setOnClickListener {
            validateAndSave()
        }
    }

    private fun validateAndSave() {
        val customerName = binding.etCustomerName.text?.toString()?.trim().orEmpty()
        val contactNumber = binding.etContactNumber.text?.toString()?.trim().orEmpty()
        val deviceType = binding.actvDeviceType.text?.toString()?.trim().orEmpty()
        val deviceBrand = binding.actvDeviceBrand.text?.toString()?.trim().orEmpty()
        val deviceModel = binding.actvDeviceModel.text?.toString()?.trim().orEmpty()
        val reportedIssue = binding.etReportedIssue.text?.toString()?.trim().orEmpty()
        val deviceCondition = binding.etDeviceCondition.text?.toString()?.trim().orEmpty()
        val accessoriesReceived = binding.etAccessoriesReceived.text?.toString()?.trim().orEmpty()

        var isValid = true

        if (customerName.isEmpty()) {
            binding.tilCustomerName.error = "Customer name is required"
            isValid = false
        } else {
            binding.tilCustomerName.error = null
        }

        if (contactNumber.isEmpty()) {
            binding.tilContactNumber.error = "Contact number is required"
            isValid = false
        } else if (contactNumber.length < 7) {
            binding.tilContactNumber.error = "Enter a valid contact number"
            isValid = false
        } else {
            binding.tilContactNumber.error = null
        }

        if (deviceBrand.isEmpty()) {
            binding.tilDeviceBrand.error = "Device brand is required"
            isValid = false
        } else {
            binding.tilDeviceBrand.error = null
        }

        if (deviceModel.isEmpty()) {
            binding.tilDeviceModel.error = "Device model is required"
            isValid = false
        } else {
            binding.tilDeviceModel.error = null
        }

        if (reportedIssue.isEmpty()) {
            binding.tilReportedIssue.error = "Reported problem description is required"
            isValid = false
        } else {
            binding.tilReportedIssue.error = null
        }

        if (deviceCondition.isEmpty()) {
            binding.tilDeviceCondition.error = "Device physical condition is required"
            isValid = false
        } else {
            binding.tilDeviceCondition.error = null
        }

        if (!isValid) return

        viewModel.createJob(
            customerName = customerName,
            contactNumber = contactNumber,
            deviceType = deviceType,
            deviceBrand = deviceBrand,
            deviceModel = deviceModel,
            reportedIssue = reportedIssue,
            deviceCondition = deviceCondition,
            accessoriesReceived = accessoriesReceived.ifEmpty { null },
            beforePhotoPath = selectedPhotoUri?.toString(),
            expectedCompletionDate = selectedDeadlineCalendar.timeInMillis
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
