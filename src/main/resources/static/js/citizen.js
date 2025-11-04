// Configuration
const API_BASE_URL = 'http://localhost:8080/api';

// Logger utility
const logger = {
    info: (message, data = null) => {
        console.log(`[INFO] ${new Date().toISOString()} - ${message}`, data || '');
    },
    error: (message, error = null) => {
        console.error(`[ERROR] ${new Date().toISOString()} - ${message}`, error || '');
    },
    success: (message, data = null) => {
        console.log(`[SUCCESS] ${new Date().toISOString()} - ${message}`, data || '');
    }
};

// Global state
let selectedTimeSlot = null;
let currentMunicipality = null;
let bookedSlots = [];
let currentBooking = null; // For edit functionality

// Initialize the page
document.addEventListener('DOMContentLoaded', () => {
    logger.info('Citizen page loaded');
    loadMunicipalities();
    setupFormValidation();
    setupFormSubmit();
    setupDatePicker();
});

// Load municipalities from the API
async function loadMunicipalities() {
    logger.info('Loading municipalities from API');
    const municipalitySelect = document.getElementById('municipality');
    
    try {
        const response = await fetch(`${API_BASE_URL}/municipalities`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const municipalities = await response.json();
        logger.success('Municipalities loaded successfully', { count: municipalities.length });
        
        // Clear loading option
        municipalitySelect.innerHTML = '<option value="">Select your municipality</option>';
        
        // Add municipalities to select
        municipalities.forEach(municipality => {
            const option = document.createElement('option');
            option.value = municipality;
            option.textContent = municipality;
            municipalitySelect.appendChild(option);
        });
        
    } catch (error) {
        logger.error('Failed to load municipalities', error);
        municipalitySelect.innerHTML = '<option value="">Error loading municipalities</option>';
        showError('Failed to load municipalities. Please refresh the page.');
    }
}

// Setup form validation
function setupFormValidation() {
    const municipalitySelect = document.getElementById('municipality');
    
    // When municipality changes, reset time slots
    municipalitySelect.addEventListener('change', () => {
        currentMunicipality = municipalitySelect.value;
        const dateInput = document.getElementById('timeSlotDate');
        if (dateInput.value && currentMunicipality) {
            loadAvailableTimeSlots(dateInput.value);
        }
    });
    
    logger.info('Form validation setup complete');
}

// Setup date picker
function setupDatePicker() {
    const dateInput = document.getElementById('timeSlotDate');
    
    // Set minimum date to tomorrow
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    dateInput.min = tomorrow.toISOString().split('T')[0];
    
    // When date changes, load available time slots
    dateInput.addEventListener('change', () => {
        if (currentMunicipality && dateInput.value) {
            loadAvailableTimeSlots(dateInput.value);
        } else if (!currentMunicipality) {
            alert('Please select a municipality first');
            dateInput.value = '';
        }
    });
    
    logger.info('Date picker setup complete', { minDate: dateInput.min });
}

// Load available time slots for the selected date and municipality
async function loadAvailableTimeSlots(date) {
    logger.info('Loading available time slots', { date, municipality: currentMunicipality });
    
    const container = document.getElementById('timeSlotsContainer');
    const slotsList = document.getElementById('timeSlotsList');
    
    container.classList.remove('hidden');
    slotsList.innerHTML = '<div class="col-span-3 text-center text-gray-500">Loading slots...</div>';
    
    try {
        // Fetch all bookings for this municipality
        const response = await fetch(`${API_BASE_URL}/bookings/municipality/${currentMunicipality}`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const bookings = await response.json();
        
        // Filter bookings for the selected date
        const selectedDate = new Date(date);
        bookedSlots = bookings
            .filter(booking => {
                const bookingDate = new Date(booking.timeSlot);
                return bookingDate.toDateString() === selectedDate.toDateString();
            })
            .map(booking => new Date(booking.timeSlot).getHours());
        
        logger.success('Booked slots loaded', { bookedSlots });
        
        // Generate time slots (9 AM to 6 PM)
        displayTimeSlots(date);
        
    } catch (error) {
        logger.error('Failed to load booked slots', error);
        slotsList.innerHTML = '<div class="col-span-3 text-center text-red-500">Error loading slots</div>';
    }
}

// Display time slots
function displayTimeSlots(date) {
    const slotsList = document.getElementById('timeSlotsList');
    slotsList.innerHTML = '';
    
    const startHour = 9;  // 9 AM
    const endHour = 18;   // 6 PM
    
    for (let hour = startHour; hour < endHour; hour++) {
        const isBooked = bookedSlots.includes(hour);
        const timeString = `${hour.toString().padStart(2, '0')}:00`;
        const dateTime = `${date}T${timeString}`;
        
        const button = document.createElement('button');
        button.type = 'button';
        button.className = `px-4 py-3 rounded-lg font-semibold transition ${
            isBooked 
                ? 'bg-gray-200 text-gray-400 cursor-not-allowed' 
                : 'bg-purple-50 text-purple-700 hover:bg-purple-600 hover:text-white border-2 border-purple-200'
        }`;
        button.textContent = timeString;
        button.disabled = isBooked;
        
        if (!isBooked) {
            button.onclick = () => selectTimeSlot(dateTime, button);
        } else {
            button.title = 'This time slot is already booked';
        }
        
        slotsList.appendChild(button);
    }
    
    logger.info('Time slots displayed', { total: endHour - startHour, booked: bookedSlots.length });
}

// Select a time slot
function selectTimeSlot(dateTime, button) {
    logger.info('Time slot selected', { dateTime });
    
    // Remove selection from all buttons
    document.querySelectorAll('#timeSlotsList button').forEach(btn => {
        btn.classList.remove('bg-purple-600', 'text-white', 'ring-4', 'ring-purple-300');
        if (!btn.disabled) {
            btn.classList.add('bg-purple-50', 'text-purple-700');
        }
    });
    
    // Add selection to clicked button
    button.classList.remove('bg-purple-50', 'text-purple-700');
    button.classList.add('bg-purple-600', 'text-white', 'ring-4', 'ring-purple-300');
    
    selectedTimeSlot = dateTime;
}

// Setup form submit handler
function setupFormSubmit() {
    const form = document.getElementById('requestForm');
    
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        logger.info('Form submission started');
        
        if (!selectedTimeSlot) {
            logger.error('No time slot selected');
            showError('Please select a time slot');
            return;
        }
        
        const formData = {
            municipality: document.getElementById('municipality').value,
            address: document.getElementById('address').value,
            timeSlot: selectedTimeSlot,
            itemDescription: document.getElementById('itemDescription').value
        };
        
        logger.info('Form data collected', formData);
        
        await submitRequest(formData);
    });
}

// Submit the request to the API
async function submitRequest(formData) {
    showLoading();
    
    try {
        logger.info('Submitting request to API', formData);
        
        const response = await fetch(`${API_BASE_URL}/bookings`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(formData)
        });
        
        const responseText = await response.text();
        
        if (!response.ok) {
            logger.error('Request submission failed', { status: response.status, message: responseText });
            throw new Error(responseText || 'Failed to submit request');
        }
        
        logger.success('Request submitted successfully', { token: responseText });
        showSuccess(responseText);
        
    } catch (error) {
        logger.error('Error submitting request', error);
        hideLoading();
        showError(error.message || 'Failed to submit request. Please try again.');
    }
}

// Show success message
function showSuccess(token) {
    hideLoading();
    logger.info('Showing success message', { token });
    
    document.getElementById('requestForm').parentElement.classList.add('hidden');
    document.getElementById('bookingToken').textContent = token;
    document.getElementById('successMessage').classList.remove('hidden');
    
    // Also show modal for tests
    document.getElementById('bookingTokenModal').textContent = token;
    document.getElementById('successModal').classList.remove('hidden');
    
    // Scroll to success message
    document.getElementById('successMessage').scrollIntoView({ behavior: 'smooth', block: 'center' });
}

// Close success modal
function closeSuccessModal() {
    document.getElementById('successModal').classList.add('hidden');
    resetForm();
}

// Show error message
function showError(message) {
    logger.error('Showing error message', { message });
    
    document.getElementById('errorText').textContent = message;
    document.getElementById('errorMessage').classList.remove('hidden');
    
    // Also show error modal for tests
    document.getElementById('errorModalText').textContent = message;
    document.getElementById('errorModal').classList.remove('hidden');
    
    // Scroll to error message
    document.getElementById('errorMessage').scrollIntoView({ behavior: 'smooth', block: 'center' });
}

// Close error modal
function closeErrorModal() {
    document.getElementById('errorModal').classList.add('hidden');
    document.getElementById('errorMessage').classList.add('hidden');
}

// Hide error message
function hideError() {
    logger.info('Hiding error message');
    document.getElementById('errorMessage').classList.add('hidden');
}

// Show loading overlay
function showLoading() {
    logger.info('Showing loading overlay');
    document.getElementById('loadingOverlay').classList.remove('hidden');
}

// Hide loading overlay
function hideLoading() {
    logger.info('Hiding loading overlay');
    document.getElementById('loadingOverlay').classList.add('hidden');
}

// Reset form and show it again
function resetForm() {
    logger.info('Resetting form');
    
    document.getElementById('requestForm').reset();
    document.getElementById('successMessage').classList.add('hidden');
    document.getElementById('errorMessage').classList.add('hidden');
    document.getElementById('requestForm').parentElement.classList.remove('hidden');
    document.getElementById('timeSlotsContainer').classList.add('hidden');
    
    selectedTimeSlot = null;
    currentMunicipality = null;
    
    // Scroll to form
    document.getElementById('requestForm').scrollIntoView({ behavior: 'smooth', block: 'start' });
    
    // Reload municipalities in case they changed
    loadMunicipalities();
}

// Token lookup functionality
async function lookupToken() {
    const token = document.getElementById('tokenLookup').value.trim();
    
    if (!token) {
        logger.error('Empty token provided');
        showError('Please enter a booking token');
        return;
    }
    
    logger.info('Looking up token', { token });
    showLoading();
    
    try {
        const response = await fetch(`${API_BASE_URL}/bookings/${token}`);
        
        if (response.status === 404) {
            logger.error('Token not found', { token });
            hideLoading();
            showError('Booking not found. Please check your token and try again.');
            return;
        }
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const booking = await response.json();
        logger.success('Booking found', booking);
        
        hideLoading();
        displayTokenResult(booking);
        
    } catch (error) {
        logger.error('Error looking up token', error);
        hideLoading();
        showError('Error retrieving booking. Please try again.');
    }
}

// Display token lookup result
function displayTokenResult(booking) {
    const content = document.getElementById('tokenResultContent');
    const statusBadge = getStatusBadge(booking.state);
    const formattedDate = formatDateTime(booking.timeSlot);
    const formattedRequestDate = formatDateTime(booking.date);
    
    content.innerHTML = `
        <div class="grid grid-cols-2 gap-4">
            <div>
                <p class="text-sm text-gray-600 mb-1">Token</p>
                <p class="font-mono text-sm bg-gray-100 p-2 rounded break-all">${booking.token}</p>
            </div>
            <div>
                <p class="text-sm text-gray-600 mb-1">Status</p>
                ${statusBadge}
            </div>
        </div>

        <div>
            <p class="text-sm text-gray-600 mb-1">Municipality</p>
            <p class="font-semibold text-lg flex items-center">
                <i class="fas fa-map-marker-alt text-purple-600 mr-2"></i>${booking.municipality}
            </p>
        </div>

        <div>
            <p class="text-sm text-gray-600 mb-1">Address</p>
            <p class="text-gray-800">${booking.address}</p>
        </div>

        <div>
            <p class="text-sm text-gray-600 mb-1">Collection Time Slot</p>
            <p class="text-gray-800 flex items-center">
                <i class="fas fa-calendar-alt text-purple-600 mr-2"></i>${formattedDate}
            </p>
        </div>

        <div>
            <p class="text-sm text-gray-600 mb-1">Item Description</p>
            <p class="text-gray-800 bg-gray-50 p-3 rounded">${booking.itemDescription}</p>
        </div>

        <div>
            <p class="text-sm text-gray-600 mb-1">Request Submitted</p>
            <p class="text-gray-800">${formattedRequestDate}</p>
        </div>
    `;
    
    document.getElementById('tokenResultModal').classList.remove('hidden');
    
    // Also show in detailsModal for tests
    document.getElementById('detailsModal').style.display = 'block';
    document.getElementById('detailsModal').innerHTML = content.innerHTML;
    
    // Store current booking for edit/delete
    currentBooking = booking;
}

// Close token result modal
function closeTokenModal() {
    logger.info('Closing token result modal');
    document.getElementById('tokenResultModal').classList.add('hidden');
    document.getElementById('detailsModal').style.display = 'none';
    document.getElementById('tokenLookup').value = '';
    currentBooking = null;
}

// Open edit modal
function openEditModal() {
    if (!currentBooking) {
        logger.error('No booking to edit');
        return;
    }
    
    logger.info('Opening edit modal', { token: currentBooking.token });
    
    // Close token modal
    document.getElementById('tokenResultModal').classList.add('hidden');
    
    // Populate edit form
    document.getElementById('editMunicipality').value = currentBooking.municipality;
    document.getElementById('editAddress').value = currentBooking.address;
    document.getElementById('editItemDescription').value = currentBooking.itemDescription;
    
    // Set date and load time slots
    const timeSlot = new Date(currentBooking.timeSlot);
    const dateStr = timeSlot.toISOString().split('T')[0];
    document.getElementById('editDatePicker').value = dateStr;
    
    // Set minimum date to today
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('editDatePicker').min = today;
    
    // Load time slots for the selected date
    currentMunicipality = currentBooking.municipality;
    loadEditTimeSlots(dateStr);
    
    // Show edit modal
    document.getElementById('editRequestModal').classList.remove('hidden');
    
    // Setup date change listener
    document.getElementById('editDatePicker').addEventListener('change', function() {
        loadEditTimeSlots(this.value);
    });
    
    // Setup form submit
    document.getElementById('editForm').onsubmit = handleEditSubmit;
}

// Close edit modal
function closeEditModal() {
    logger.info('Closing edit modal');
    document.getElementById('editRequestModal').classList.add('hidden');
    selectedTimeSlot = null;
}

// Load time slots for edit
async function loadEditTimeSlots(selectedDate) {
    if (!selectedDate || !currentMunicipality) {
        document.getElementById('editTimeSlotsSection').classList.add('hidden');
        return;
    }
    
    logger.info('Loading time slots for edit', { date: selectedDate, municipality: currentMunicipality });
    
    try {
        // Fetch bookings for selected municipality
        const response = await fetch(`${API_BASE_URL}/bookings/municipality/${encodeURIComponent(currentMunicipality)}`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const bookings = await response.json();
        
        // Filter bookings for selected date
        bookedSlots = bookings
            .filter(booking => {
                const bookingDate = new Date(booking.timeSlot).toISOString().split('T')[0];
                return bookingDate === selectedDate && booking.token !== currentBooking.token; // Exclude current booking
            })
            .map(booking => {
                const time = new Date(booking.timeSlot);
                return `${String(time.getHours()).padStart(2, '0')}:${String(time.getMinutes()).padStart(2, '0')}`;
            });
        
        logger.info('Booked slots loaded', { count: bookedSlots.length, slots: bookedSlots });
        
        // Generate time slots
        generateEditTimeSlots();
        
    } catch (error) {
        logger.error('Failed to load booked slots', error);
        generateEditTimeSlots(); // Generate slots anyway
    }
}

// Generate time slots for edit
function generateEditTimeSlots() {
    const timeSlotsList = document.getElementById('editTimeSlotsList');
    timeSlotsList.innerHTML = '';
    
    // Generate slots from 8:00 to 18:00
    for (let hour = 8; hour <= 18; hour++) {
        for (let minute = 0; minute < 60; minute += 30) {
            const timeString = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
            const isBooked = bookedSlots.includes(timeString);
            const isCurrentSlot = currentBooking && 
                new Date(currentBooking.timeSlot).toTimeString().substring(0, 5) === timeString;
            
            const button = document.createElement('button');
            button.type = 'button';
            button.className = `px-4 py-3 rounded-lg text-sm font-semibold transition ${
                isBooked && !isCurrentSlot
                    ? 'bg-gray-200 text-gray-400 cursor-not-allowed'
                    : isCurrentSlot
                    ? 'bg-purple-600 text-white border-2 border-purple-700'
                    : 'bg-white border-2 border-gray-200 text-gray-700 hover:border-purple-500 hover:bg-purple-50'
            }`;
            button.textContent = timeString;
            button.disabled = isBooked && !isCurrentSlot;
            
            if (!button.disabled || isCurrentSlot) {
                button.onclick = () => selectEditTimeSlot(timeString, button);
            }
            
            // Pre-select current time slot
            if (isCurrentSlot) {
                selectedTimeSlot = timeString;
            }
            
            timeSlotsList.appendChild(button);
        }
    }
    
    document.getElementById('editTimeSlotsSection').classList.remove('hidden');
}

// Select edit time slot
function selectEditTimeSlot(timeString, buttonElement) {
    selectedTimeSlot = timeString;
    logger.info('Time slot selected for edit', { timeSlot: timeString });
    
    // Update UI
    document.querySelectorAll('#editTimeSlotsList button').forEach(btn => {
        btn.classList.remove('bg-purple-600', 'text-white', 'border-purple-700');
        btn.classList.add('bg-white', 'border-gray-200', 'text-gray-700');
    });
    
    buttonElement.classList.remove('bg-white', 'border-gray-200', 'text-gray-700');
    buttonElement.classList.add('bg-purple-600', 'text-white', 'border-purple-700');
}

// Handle edit form submit
async function handleEditSubmit(e) {
    e.preventDefault();
    
    if (!selectedTimeSlot) {
        showError('Please select a time slot');
        return;
    }
    
    const selectedDate = document.getElementById('editDatePicker').value;
    const [hours, minutes] = selectedTimeSlot.split(':');
    const timeSlotDate = new Date(selectedDate);
    timeSlotDate.setHours(parseInt(hours), parseInt(minutes), 0, 0);
    
    // Validate time slot is at least 1 hour from now
    const minTime = new Date();
    minTime.setHours(minTime.getHours() + 1);
    
    if (timeSlotDate < minTime) {
        logger.error('Invalid time slot - less than 1 hour from now');
        showError('Time slot must be at least 1 hour from now.');
        return;
    }
    
    const updatedRequest = {
        municipality: currentBooking.municipality,
        address: document.getElementById('editAddress').value,
        timeSlot: timeSlotDate.toISOString(),
        itemDescription: document.getElementById('editItemDescription').value,
        state: currentBooking.state,
        date: currentBooking.date
    };
    
    logger.info('Updating request', { token: currentBooking.token, updatedRequest });
    
    showLoading();
    
    try {
        const response = await fetch(`${API_BASE_URL}/bookings/${currentBooking.token}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(updatedRequest)
        });
        
        const responseText = await response.text();
        
        if (!response.ok) {
            logger.error('Update failed', { status: response.status, message: responseText });
            throw new Error(responseText || 'Failed to update request');
        }
        
        logger.success('Request updated successfully', { token: currentBooking.token });
        
        hideLoading();
        closeEditModal();
        
        // Show success message
        alert('Request updated successfully!');
        
        // Reload the booking to show updated data
        lookupByToken(currentBooking.token);
        
    } catch (error) {
        logger.error('Error updating request', error);
        hideLoading();
        showError(error.message || 'Failed to update request. Please try again.');
    }
}

// Confirm delete request
function confirmDeleteRequest() {
    if (!currentBooking) {
        logger.error('No booking to delete');
        return;
    }
    
    logger.info('Opening delete confirmation modal');
    
    // Close token modal
    document.getElementById('tokenResultModal').classList.add('hidden');
    
    // Show delete confirmation
    document.getElementById('deleteConfirmModal').classList.remove('hidden');
}

// Close delete modal
function closeDeleteModal() {
    logger.info('Closing delete confirmation modal');
    document.getElementById('deleteConfirmModal').classList.add('hidden');
}

// Execute delete
async function executeDelete() {
    if (!currentBooking) {
        logger.error('No booking to delete');
        return;
    }
    
    logger.info('Deleting request', { token: currentBooking.token });
    
    closeDeleteModal();
    showLoading();
    
    try {
        const response = await fetch(`${API_BASE_URL}/bookings/${currentBooking.token}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            throw new Error('Failed to delete request');
        }
        
        logger.success('Request deleted successfully', { token: currentBooking.token });
        
        hideLoading();
        currentBooking = null;
        
        // Show success message
        alert('Request deleted successfully!');
        
        // Reset form
        document.getElementById('tokenLookup').value = '';
        
    } catch (error) {
        logger.error('Error deleting request', error);
        hideLoading();
        showError(error.message || 'Failed to delete request. Please try again.');
    }
}

// Get status badge
function getStatusBadge(status) {
    const badges = {
        'RECEIVED': '<span class="inline-block px-3 py-1 rounded-full text-sm font-semibold bg-blue-100 text-blue-800">Received</span>',
        'ASSIGNED': '<span class="inline-block px-3 py-1 rounded-full text-sm font-semibold bg-yellow-100 text-yellow-800">Assigned</span>',
        'IN_PROGRESS': '<span class="inline-block px-3 py-1 rounded-full text-sm font-semibold bg-orange-100 text-orange-800">In Progress</span>',
        'COMPLETED': '<span class="inline-block px-3 py-1 rounded-full text-sm font-semibold bg-green-100 text-green-800">Completed</span>',
        'CANCELLED': '<span class="inline-block px-3 py-1 rounded-full text-sm font-semibold bg-red-100 text-red-800">Cancelled</span>'
    };
    return badges[status] || status;
}

// Format date time
function formatDateTime(dateTimeString) {
    const date = new Date(dateTimeString);
    return date.toLocaleString('en-GB', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}
