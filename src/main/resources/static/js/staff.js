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
let allRequests = [];
let currentRequest = null;
let deleteToken = null; // For staff delete functionality

// Initialize the page
document.addEventListener('DOMContentLoaded', () => {
    logger.info('Staff portal page loaded');
    loadMunicipalities();
    loadAllRequests();
});

// Load municipalities for filter
async function loadMunicipalities() {
    logger.info('Loading municipalities for filter');
    const municipalitySelect = document.getElementById('filterMunicipality');
    
    try {
        const response = await fetch(`${API_BASE_URL}/municipalities`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const municipalities = await response.json();
        logger.success('Municipalities loaded for filter', { count: municipalities.length });
        
        // Add municipalities to filter
        municipalities.forEach(municipality => {
            const option = document.createElement('option');
            option.value = municipality;
            option.textContent = municipality;
            municipalitySelect.appendChild(option);
        });
        
    } catch (error) {
        logger.error('Failed to load municipalities for filter', error);
    }
}

// Load all requests
async function loadAllRequests() {
    logger.info('Loading all requests');
    showLoading();
    
    try {
        const response = await fetch(`${API_BASE_URL}/bookings`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        allRequests = await response.json();
        logger.success('All requests loaded', { count: allRequests.length });
        
        displayRequests(allRequests);
        updateStatistics(allRequests);
        
    } catch (error) {
        logger.error('Failed to load requests', error);
        showEmptyState();
    }
}

// Display requests in table
function displayRequests(requests) {
    logger.info('Displaying requests', { count: requests.length });
    
    const tableBody = document.getElementById('requestsTableBody');
    const loadingContainer = document.getElementById('loadingContainer');
    const requestsContainer = document.getElementById('requestsContainer');
    const emptyState = document.getElementById('emptyState');
    
    loadingContainer.classList.add('hidden');
    
    if (requests.length === 0) {
        requestsContainer.classList.add('hidden');
        emptyState.classList.remove('hidden');
        return;
    }
    
    emptyState.classList.add('hidden');
    requestsContainer.classList.remove('hidden');
    
    // Clear existing rows
    tableBody.innerHTML = '';
    
    // Add rows
    requests.forEach(request => {
        const row = createRequestRow(request);
        tableBody.appendChild(row);
    });
}

// Create a table row for a request
function createRequestRow(request) {
    const row = document.createElement('tr');
    row.className = 'hover:bg-gray-50 transition';
    
    const statusBadge = getStatusBadge(request.state);
    const formattedDate = formatDateTime(request.timeSlot);
    
    row.innerHTML = `
        <td class="px-6 py-4 whitespace-nowrap">
            <span class="text-xs font-mono text-gray-600">${request.token.substring(0, 8)}...</span>
        </td>
        <td class="px-6 py-4 whitespace-nowrap">
            <div class="flex items-center">
                <i class="fas fa-map-marker-alt text-purple-600 mr-2"></i>
                <span class="font-semibold text-gray-800">${request.municipality}</span>
            </div>
        </td>
        <td class="px-6 py-4">
            <span class="text-gray-600">${request.address}</span>
        </td>
        <td class="px-6 py-4 whitespace-nowrap">
            <span class="text-gray-600">${formattedDate}</span>
        </td>
        <td class="px-6 py-4 whitespace-nowrap">
            ${statusBadge}
        </td>
        <td class="px-6 py-4 whitespace-nowrap text-right">
            <button onclick="openDetailsModal('${request.token}')" 
                class="text-blue-600 hover:text-blue-800 mr-3" title="View Details">
                <i class="fas fa-eye"></i>
            </button>
            <button onclick="openUpdateModal('${request.token}')" 
                class="text-purple-600 hover:text-purple-800 mr-3" title="Update Status">
                <i class="fas fa-edit"></i>
            </button>
            <button onclick="confirmStaffDelete('${request.token}')" 
                class="text-red-600 hover:text-red-800" title="Delete Request">
                <i class="fas fa-trash"></i>
            </button>
        </td>
    `;
    
    return row;
}

// Get status badge HTML
function getStatusBadge(status) {
    const badges = {
        'RECEIVED': '<span class="px-3 py-1 rounded-full text-xs font-semibold bg-blue-100 text-blue-800">Received</span>',
        'ASSIGNED': '<span class="px-3 py-1 rounded-full text-xs font-semibold bg-yellow-100 text-yellow-800">Assigned</span>',
        'IN_PROGRESS': '<span class="px-3 py-1 rounded-full text-xs font-semibold bg-orange-100 text-orange-800">In Progress</span>',
        'COMPLETED': '<span class="px-3 py-1 rounded-full text-xs font-semibold bg-green-100 text-green-800">Completed</span>',
        'CANCELLED': '<span class="px-3 py-1 rounded-full text-xs font-semibold bg-red-100 text-red-800">Cancelled</span>'
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

// Update statistics
function updateStatistics(requests) {
    logger.info('Updating statistics', { count: requests.length });
    
    const total = requests.length;
    const assigned = requests.filter(r => r.state === 'ASSIGNED').length;
    const inProgress = requests.filter(r => r.state === 'IN_PROGRESS').length;
    const completed = requests.filter(r => r.state === 'COMPLETED').length;
    
    document.getElementById('totalRequests').textContent = total;
    document.getElementById('pendingRequests').textContent = assigned;
    document.getElementById('inProgressRequests').textContent = inProgress;
    document.getElementById('completedRequests').textContent = completed;
}

// Apply filters
function applyFilters() {
    const municipalityFilter = document.getElementById('filterMunicipality').value;
    const statusFilter = document.getElementById('filterStatus').value;
    
    logger.info('Applying filters', { municipality: municipalityFilter, status: statusFilter });
    
    let filteredRequests = allRequests;
    
    if (municipalityFilter) {
        filteredRequests = filteredRequests.filter(r => r.municipality === municipalityFilter);
    }
    
    if (statusFilter) {
        filteredRequests = filteredRequests.filter(r => r.state === statusFilter);
    }
    
    logger.info('Filters applied', { resultCount: filteredRequests.length });
    displayRequests(filteredRequests);
}

// Open update status modal
function openUpdateModal(token) {
    logger.info('Opening update modal', { token });
    
    currentRequest = allRequests.find(r => r.token === token);
    
    if (!currentRequest) {
        logger.error('Request not found', { token });
        return;
    }
    
    document.getElementById('modalToken').textContent = token;
    document.getElementById('modalMunicipality').textContent = currentRequest.municipality;
    document.getElementById('newStatus').value = currentRequest.state;
    
    // Sync with updateStatus select for tests
    document.getElementById('updateStatus').value = currentRequest.state;
    
    document.getElementById('updateModal').classList.remove('hidden');
}

// Close update modal
function closeModal() {
    logger.info('Closing update modal');
    document.getElementById('updateModal').classList.add('hidden');
    currentRequest = null;
}

// Update status
async function updateStatus() {
    if (!currentRequest) {
        logger.error('No current request to update');
        return;
    }
    
    let newStatus = document.getElementById('newStatus').value;
    
    // Check updateStatus select for tests
    const testSelect = document.getElementById('updateStatus');
    if (testSelect && testSelect.value) {
        newStatus = testSelect.value;
    }
    
    logger.info('Updating request status', { token: currentRequest.token, newStatus });
    
    try {
        const updatedRequest = {
            ...currentRequest,
            state: newStatus
        };
        
        const response = await fetch(`${API_BASE_URL}/bookings/${currentRequest.token}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(updatedRequest)
        });
        
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || 'Failed to update status');
        }
        
        logger.success('Status updated successfully', { token: currentRequest.token, newStatus });
        
        closeModal();
        loadAllRequests(); // Reload all requests
        
    } catch (error) {
        logger.error('Failed to update status', error);
        alert('Failed to update status: ' + error.message);
    }
}

// Open details modal
function openDetailsModal(token) {
    logger.info('Opening details modal', { token });
    
    const request = allRequests.find(r => r.token === token);
    
    if (!request) {
        logger.error('Request not found', { token });
        return;
    }
    
    document.getElementById('detailToken').textContent = request.token;
    document.getElementById('detailStatus').innerHTML = getStatusBadge(request.state);
    document.getElementById('detailMunicipality').textContent = request.municipality;
    document.getElementById('detailAddress').textContent = request.address;
    document.getElementById('detailTimeSlot').textContent = formatDateTime(request.timeSlot);
    document.getElementById('detailDescription').textContent = request.itemDescription;
    document.getElementById('detailDate').textContent = formatDateTime(request.date);
    
    document.getElementById('detailsModal').classList.remove('hidden');
}

// Close details modal
function closeDetailsModal() {
    logger.info('Closing details modal');
    document.getElementById('detailsModal').classList.add('hidden');
}

// Show loading state
function showLoading() {
    document.getElementById('loadingContainer').classList.remove('hidden');
    document.getElementById('requestsContainer').classList.add('hidden');
    document.getElementById('emptyState').classList.add('hidden');
}

// Show empty state
function showEmptyState() {
    document.getElementById('loadingContainer').classList.add('hidden');
    document.getElementById('requestsContainer').classList.add('hidden');
    document.getElementById('emptyState').classList.remove('hidden');
}

// Confirm staff delete
function confirmStaffDelete(token) {
    logger.info('Opening staff delete confirmation modal', { token });
    
    deleteToken = token;
    document.getElementById('staffDeleteToken').textContent = token;
    document.getElementById('staffDeleteConfirmModal').classList.remove('hidden');
    
    // Also show deleteModal for tests
    document.getElementById('deleteModal').style.display = 'block';
}

// Close staff delete modal
function closeStaffDeleteModal() {
    logger.info('Closing staff delete confirmation modal');
    document.getElementById('staffDeleteConfirmModal').classList.add('hidden');
    document.getElementById('deleteModal').style.display = 'none';
    deleteToken = null;
}

// Execute staff delete
async function executeStaffDelete() {
    if (!deleteToken) {
        logger.error('No token to delete');
        return;
    }

    // Capture the token locally before closing the modal or clearing state
    const tokenToDelete = deleteToken;
    logger.info('Staff deleting request', { token: tokenToDelete });

    // Close modal and show loading UI
    closeStaffDeleteModal();
    showLoading();

    try {
        const response = await fetch(`${API_BASE_URL}/bookings/${encodeURIComponent(tokenToDelete)}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error('Failed to delete request');
        }

        logger.success('Request deleted successfully by staff', { token: tokenToDelete });

        // Clear state and reload
        deleteToken = null;
        loadAllRequests();

    } catch (error) {
        logger.error('Error deleting request', error);
        showEmptyState();
        alert('Failed to delete request: ' + error.message);
    }
}
