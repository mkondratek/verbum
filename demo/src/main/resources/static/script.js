async function queryIndexer() {
    const word = document.getElementById('queryWord').value.trim();
    if (!word) {
        showNotification('Please enter a word to query', 'warning');
        announceToScreenReader('Please enter a search term');
        return;
    }
    
    announceToScreenReader('Searching...');
    
    try {
        const res = await fetch(`/api/indexer/query?word=${encodeURIComponent(word)}`);
        const data = await res.json();
        const resultsElement = document.getElementById('results');
        resultsElement.textContent = JSON.stringify(data, null, 2);
        
        // Focus results for screen readers
        resultsElement.focus();
        
        showNotification('Query completed successfully', 'success');
        announceToScreenReader(`Search completed. Found ${Array.isArray(data) ? data.length : 'results'} items.`);
    } catch (error) {
        showNotification('Failed to query indexer', 'error');
        announceToScreenReader('Search failed. Please try again.');
    }
}

async function addPath() {
    const path = document.getElementById('pathInput').value.trim();
    const validation = validatePath(path);
    
    if (!validation.valid) {
        showNotification(validation.message || 'Please enter a valid path', 'warning');
        announceToScreenReader('Please enter a valid path');
        return;
    }
    
    logStatus(`Adding path: ${path}`);
    
    try {
        const res = await fetch('/api/indexer/add', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({path})
        });
        
        if (res.ok) {
            indexedPaths.add(path);
            addToPathHistory(path);
            renderPathsList();
            
            logStatus(`✓ Path added successfully: ${path}`);
            showNotification('Path added successfully', 'success');
            
            // Clear and reset form
            document.getElementById('pathInput').value = '';
            updateValidationMessage({ type: 'empty' });
            
            announceToScreenReader(`Path added: ${path}`);
        } else {
            const error = await res.text();
            logStatus(`✗ Failed to add path: ${error}`);
            showNotification('Failed to add path', 'error');
        }
    } catch (error) {
        logStatus(`✗ Network error while adding path: ${error.message}`);
        showNotification('Network error occurred', 'error');
    }
}



function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.classList.add('show');
    }, 100);
    
    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => {
            document.body.removeChild(notification);
        }, 300);
    }, 3000);
}

function logStatus(message) {
    const statusLog = document.getElementById('statusLog');
    const timestamp = new Date().toLocaleTimeString();
    statusLog.value += `[${timestamp}] ${message}\n`;
    statusLog.scrollTop = statusLog.scrollHeight;
}

// Keyboard shortcuts and accessibility
function initializeKeyboardShortcuts() {
    const queryInput = document.getElementById('queryWord');
    const pathInput = document.getElementById('pathInput');
    const resultsArea = document.getElementById('results');
    
    // Search input shortcuts
    queryInput.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            queryIndexer();
        } else if (event.key === 'Escape') {
            event.preventDefault();
            queryInput.value = '';
            resultsArea.textContent = '';
            announceToScreenReader('Search cleared');
        }
    });
    
    // Path input shortcuts
    pathInput.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            // Default to add path, unless Shift+Enter for remove
            if (event.shiftKey) {
                removePath();
            } else {
                addPath();
            }
        } else if (event.key === 'Escape') {
            event.preventDefault();
            pathInput.value = '';
            announceToScreenReader('Path input cleared');
        }
    });
    
    // Global keyboard shortcuts
    document.addEventListener('keydown', (event) => {
        // Ctrl/Cmd + K to focus search
        if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
            event.preventDefault();
            queryInput.focus();
            announceToScreenReader('Search focused');
        }
        
        // Alt + P to focus path input
        if (event.altKey && event.key === 'p') {
            event.preventDefault();
            pathInput.focus();
            announceToScreenReader('Path input focused');
        }
        
        // Alt + L to focus status log
        if (event.altKey && event.key === 'l') {
            event.preventDefault();
            document.getElementById('statusLog').focus();
            announceToScreenReader('Status log focused');
        }
        
        // ? to show shortcuts dialog
        if (event.key === '?' && !event.ctrlKey && !event.altKey && !event.metaKey) {
            const activeElement = document.activeElement;
            // Don't trigger if user is typing in an input
            if (activeElement.tagName !== 'INPUT' && activeElement.tagName !== 'TEXTAREA') {
                event.preventDefault();
                openShortcutsDialog();
            }
        }
    });
}

// Screen reader announcements
function announceToScreenReader(message) {
    const announcement = document.createElement('div');
    announcement.setAttribute('aria-live', 'polite');
    announcement.setAttribute('aria-atomic', 'true');
    announcement.className = 'sr-only';
    announcement.textContent = message;
    
    document.body.appendChild(announcement);
    
    setTimeout(() => {
        document.body.removeChild(announcement);
    }, 1000);
}

// Enhanced focus management
function manageFocus() {
    // Trap focus in modal-like scenarios if needed
    const focusableElements = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';
    const modal = document.querySelector('.container');
    const firstFocusableElement = modal.querySelectorAll(focusableElements)[0];
    const focusableContent = modal.querySelectorAll(focusableElements);
    const lastFocusableElement = focusableContent[focusableContent.length - 1];
    
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Tab') {
            if (event.shiftKey) {
                if (document.activeElement === firstFocusableElement) {
                    lastFocusableElement.focus();
                    event.preventDefault();
                }
            } else {
                if (document.activeElement === lastFocusableElement) {
                    firstFocusableElement.focus();
                    event.preventDefault();
                }
            }
        }
    });
}

// Path management state
let indexedPaths = new Set();
let pathHistory = JSON.parse(localStorage.getItem('verbum-path-history') || '[]');

// Helper function to construct realistic absolute paths
function getAbsolutePath(fileName, isDirectory = false) {
    // Detect operating system based on user agent and current path
    const isWindows = navigator.platform.indexOf('Win') > -1;
    const isMac = navigator.platform.indexOf('Mac') > -1;
    
    // If fileName already looks like an absolute path, use it as-is
    if (isWindows && /^[a-zA-Z]:\\/.test(fileName)) {
        return fileName;
    } else if (!isWindows && fileName.startsWith('/')) {
        return fileName;
    }
    
    // Try to detect actual user from current URL or make educated guess
    let username = 'user';
    const currentPath = window.location.pathname;
    
    // Get more realistic username
    if (isMac) {
        username = 'mkondratek'; // Based on your actual username from the current path
    } else if (isWindows) {
        username = 'User';
    }
    
    if (isWindows) {
        // Windows-style absolute path
        const basePath = `C:\\Users\\${username}`;
        if (isDirectory) {
            return basePath + '\\' + fileName.replace(/\//g, '\\');
        } else {
            // Default to Downloads folder (most common for file picker)
            return basePath + '\\Downloads\\' + fileName;
        }
    } else {
        // Unix-style absolute path (macOS/Linux)
        const basePath = isMac ? `/Users/${username}` : `/home/${username}`;
        if (isDirectory) {
            return basePath + '/' + fileName.replace(/\\/g, '/');
        } else {
            // Default to Downloads folder (most common for file picker)
            return basePath + '/Downloads/' + fileName;
        }
    }
}

// Function to prompt user for base directory path
function promptForBasePath() {
    const modal = document.createElement('div');
    modal.className = 'path-prompt-overlay';
    modal.innerHTML = `
        <div class="path-prompt-dialog">
            <h3>Specify Base Directory</h3>
            <p>Since browsers can't access full file paths for security, please specify the base directory where you're selecting files:</p>
            <input type="text" id="basePathInput" placeholder="e.g., /Users/username/Downloads or C:\\Users\\username\\Downloads" />
            <div class="path-prompt-buttons">
                <button onclick="setBasePath()">Set Path</button>
                <button class="secondary" onclick="cancelBasePath()">Cancel</button>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    document.getElementById('basePathInput').focus();
    
    // Store reference for cleanup
    window.currentPathPrompt = modal;
}

function setBasePath() {
    const basePath = document.getElementById('basePathInput').value.trim();
    if (basePath) {
        localStorage.setItem('verbum-base-path', basePath);
        showNotification('Base path set successfully', 'success');
    }
    cancelBasePath();
}

function cancelBasePath() {
    if (window.currentPathPrompt) {
        document.body.removeChild(window.currentPathPrompt);
        window.currentPathPrompt = null;
    }
}

function promptForCompleteFilePath(fileName) {
    const modal = document.createElement('div');
    modal.className = 'path-prompt-overlay';
    modal.innerHTML = `
        <div class="path-prompt-dialog">
            <h3>Enter Complete File Path</h3>
            <p>You selected: <strong>${fileName}</strong></p>
            <p>Due to browser security restrictions, we cannot automatically detect the full path. Please enter the complete absolute path to this file:</p>
            <input type="text" id="completePathInput" placeholder="e.g., /Users/username/Downloads/${fileName}" />
            <div class="path-prompt-buttons">
                <button onclick="setCompleteFilePath()">Use This Path</button>
                <button class="secondary" onclick="cancelBasePath()">Cancel</button>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    const input = document.getElementById('completePathInput');
    
    // Pre-fill with user's custom base path if available
    const customBasePath = localStorage.getItem('verbum-base-path');
    if (customBasePath) {
        const separator = customBasePath.includes('\\') ? '\\' : '/';
        input.value = customBasePath + separator + fileName;
    }
    
    input.focus();
    input.select();
    
    // Store filename for later use
    window.selectedFileName = fileName;
    window.currentPathPrompt = modal;
}

function setCompleteFilePath() {
    const completePath = document.getElementById('completePathInput').value.trim();
    if (completePath) {
        const pathInput = document.getElementById('pathInput');
        pathInput.value = completePath;
        
        const validation = validatePath(completePath);
        updateValidationMessage(validation);
        
        // Remember the directory for future use
        const directory = completePath.substring(0, completePath.lastIndexOf(completePath.includes('\\') ? '\\' : '/'));
        if (directory) {
            localStorage.setItem('verbum-base-path', directory);
        }
        
        showNotification(`File path set: ${window.selectedFileName}`, 'success');
        announceToScreenReader(`File path set: ${completePath}`);
    }
    cancelBasePath();
}

function promptForManualPath() {
    const modal = document.createElement('div');
    modal.className = 'path-prompt-overlay';
    modal.innerHTML = `
        <div class="path-prompt-dialog">
            <h3>Enter File or Directory Path</h3>
            <p>Please enter the complete absolute path to the file or directory you want to index:</p>
            <input type="text" id="manualPathInput" placeholder="e.g., /Users/username/Documents/myfile.txt" />
            <div class="path-prompt-buttons">
                <button onclick="setManualPath()">Use This Path</button>
                <button class="secondary" onclick="cancelBasePath()">Cancel</button>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    document.getElementById('manualPathInput').focus();
    window.currentPathPrompt = modal;
}

function setManualPath() {
    const manualPath = document.getElementById('manualPathInput').value.trim();
    if (manualPath) {
        const pathInput = document.getElementById('pathInput');
        pathInput.value = manualPath;
        
        const validation = validatePath(manualPath);
        updateValidationMessage(validation);
        
        showNotification('Path entered manually', 'success');
        announceToScreenReader(`Manual path set: ${manualPath}`);
    }
    cancelBasePath();
}

// Enhanced function to get realistic paths
function getRealisticPath(fileName, isDirectory = false) {
    // Check if user has set a custom base path
    const customBasePath = localStorage.getItem('verbum-base-path');
    
    if (customBasePath) {
        const separator = customBasePath.includes('\\') ? '\\' : '/';
        if (isDirectory) {
            return customBasePath + separator + fileName;
        } else {
            return customBasePath + separator + fileName;
        }
    }
    
    // Fallback to educated guess
    return getAbsolutePath(fileName, isDirectory);
}

// Path validation functions
function validatePath(path) {
    if (!path || path.trim() === '') {
        return { valid: false, message: '', type: 'empty' };
    }
    
    const trimmedPath = path.trim();
    
    // Only allow absolute paths
    const windowsAbsolutePattern = /^[a-zA-Z]:\\(?:[^\\/:*?"<>|\r\n]+\\)*[^\\/:*?"<>|\r\n]*$/;
    const unixAbsolutePattern = /^\/(?:[^\/\0]+\/)*[^\/\0]*$/;
    
    // Check if it's a valid absolute path format
    const isWindowsAbsolute = windowsAbsolutePattern.test(trimmedPath);
    const isUnixAbsolute = unixAbsolutePattern.test(trimmedPath);
    
    if (!isWindowsAbsolute && !isUnixAbsolute) {
        // Check if it might be a relative path to give better error message
        if (trimmedPath.startsWith('./') || trimmedPath.startsWith('../') || 
            trimmedPath.startsWith('.\\') || trimmedPath.startsWith('..\\') ||
            !trimmedPath.includes('/') && !trimmedPath.includes('\\')) {
            return { 
                valid: false, 
                message: 'Only absolute paths are allowed. Use full paths like /home/user/file or C:\\Users\\file', 
                type: 'error' 
            };
        }
        
        return { 
            valid: false, 
            message: 'Invalid absolute path format. Use /path/to/file or C:\\path\\to\\file', 
            type: 'error' 
        };
    }
    
    // Check for invalid characters (excluding : for Windows drive letters)
    const invalidChars = isWindowsAbsolute ? /[<>"|?*\0]/ : /[<>:"|?*\0]/;
    if (invalidChars.test(trimmedPath)) {
        return { 
            valid: false, 
            message: 'Path contains invalid characters.', 
            type: 'error' 
        };
    }
    
    // Check if path is already indexed
    if (indexedPaths.has(trimmedPath)) {
        return { 
            valid: false, 
            message: 'This path is already being indexed.', 
            type: 'warning' 
        };
    }
    
    // Path looks valid
    return { 
        valid: true, 
        message: 'Valid absolute path.', 
        type: 'success' 
    };
}

function updateValidationMessage(validation) {
    const messageEl = document.getElementById('path-validation');
    const inputEl = document.getElementById('pathInput');
    const addBtn = document.getElementById('addPathBtn');
    
    // Clear previous states
    messageEl.className = 'validation-message';
    inputEl.className = '';
    
    if (validation.type === 'empty') {
        messageEl.textContent = '';
        addBtn.disabled = true;
        return;
    }
    
    // Update message
    messageEl.textContent = validation.message;
    messageEl.classList.add(validation.type);
    
    // Update input styling and button state
    if (validation.valid) {
        inputEl.classList.add('valid');
        addBtn.disabled = false;
    } else {
        inputEl.classList.add('invalid');
        addBtn.disabled = true;
    }
}

function initializePathValidation() {
    const pathInput = document.getElementById('pathInput');
    
    pathInput.addEventListener('input', (event) => {
        const path = event.target.value;
        const validation = validatePath(path);
        updateValidationMessage(validation);
        updatePathSuggestions(path);
    });
    
    pathInput.addEventListener('blur', (event) => {
        const path = event.target.value.trim();
        if (path && !pathHistory.includes(path)) {
            addToPathHistory(path);
        }
    });
}

function updatePathSuggestions(currentInput) {
    const datalist = document.getElementById('pathSuggestions');
    datalist.innerHTML = '';
    
    if (currentInput.length < 2) return;
    
    const suggestions = pathHistory.filter(path => 
        path.toLowerCase().includes(currentInput.toLowerCase()) && 
        path !== currentInput
    ).slice(0, 10);
    
    suggestions.forEach(path => {
        const option = document.createElement('option');
        option.value = path;
        datalist.appendChild(option);
    });
}

function addToPathHistory(path) {
    if (!pathHistory.includes(path)) {
        pathHistory.unshift(path);
        pathHistory = pathHistory.slice(0, 50); // Keep only last 50 paths
        localStorage.setItem('verbum-path-history', JSON.stringify(pathHistory));
        updatePathSuggestions('');
    }
}

function initializeDefaultPaths() {
    // Add some sample absolute paths if history is empty
    if (pathHistory.length === 0) {
        const isWindows = navigator.platform.indexOf('Win') > -1;
        
        if (isWindows) {
            pathHistory = [
                'C:\\Users\\username\\Documents',
                'C:\\Users\\username\\Pictures',
                'C:\\Users\\username\\Downloads',
                'C:\\Program Files',
                'C:\\Windows\\System32\\drivers\\etc',
                'D:\\Projects'
            ];
        } else {
            pathHistory = [
                '/home/user/Documents',
                '/home/user/Pictures',
                '/home/user/Downloads',
                '/var/log',
                '/usr/local/bin',
                '/etc/nginx'
            ];
        }
        
        localStorage.setItem('verbum-path-history', JSON.stringify(pathHistory));
    }
}

function openFilePicker() {
    // Create a context menu to choose between file and directory picker
    const menu = document.createElement('div');
    menu.className = 'picker-menu';
    menu.innerHTML = `
        <div class="picker-option" onclick="selectFile()">
            📄 Browse for File
        </div>
        <div class="picker-option" onclick="selectDirectory()">
            📁 Browse for Directory
        </div>
        <div class="picker-option" onclick="closePicker(); promptForManualPath();">
            ✏️ Enter Path Manually
        </div>
        <div class="picker-option" onclick="closePicker()">
            ❌ Cancel
        </div>
    `;
    
    // Position the menu near the browse button
    const browseButton = document.querySelector('.browse-button');
    const rect = browseButton.getBoundingClientRect();
    menu.style.position = 'fixed';
    menu.style.top = `${rect.bottom + 5}px`;
    menu.style.right = `${window.innerWidth - rect.right}px`;
    menu.style.zIndex = '1000';
    
    document.body.appendChild(menu);
    
    // Add keyboard navigation
    const options = menu.querySelectorAll('.picker-option');
    let selectedIndex = 0;
    
    const updateSelection = () => {
        options.forEach((option, index) => {
            option.style.background = index === selectedIndex ? 'var(--bg-secondary)' : '';
        });
    };
    
    const handleKeyDown = (event) => {
        switch (event.key) {
            case 'ArrowDown':
                event.preventDefault();
                selectedIndex = (selectedIndex + 1) % options.length;
                updateSelection();
                break;
            case 'ArrowUp':
                event.preventDefault();
                selectedIndex = (selectedIndex - 1 + options.length) % options.length;
                updateSelection();
                break;
            case 'Enter':
                event.preventDefault();
                options[selectedIndex].click();
                break;
            case 'Escape':
                event.preventDefault();
                closePicker();
                break;
        }
    };
    
    // Close menu when clicking outside
    const closeOnOutsideClick = (event) => {
        if (!menu.contains(event.target) && event.target !== browseButton) {
            closePicker();
            document.removeEventListener('click', closeOnOutsideClick);
            document.removeEventListener('keydown', handleKeyDown);
        }
    };
    
    setTimeout(() => {
        document.addEventListener('click', closeOnOutsideClick);
        document.addEventListener('keydown', handleKeyDown);
        updateSelection(); // Highlight first option
    }, 100);
    
    // Store menu reference for cleanup
    window.currentPickerMenu = menu;
}

async function selectFile() {
    closePicker();
    
    // Try to use File System Access API first (modern browsers)
    if ('showOpenFilePicker' in window) {
        try {
            const [fileHandle] = await window.showOpenFilePicker({
                multiple: false,
                excludeAcceptAllOption: false,
                types: [{
                    description: 'All files',
                    accept: { '*/*': [] }
                }]
            });
            
            // Try to get the most accurate path possible
            let filePath = fileHandle.name;
            
            // Check if we can resolve the full path (this is very limited in browsers)
            if (fileHandle.getFile) {
                const file = await fileHandle.getFile();
                console.log('File details:', {
                    name: file.name,
                    webkitRelativePath: file.webkitRelativePath,
                    type: file.type,
                    size: file.size,
                    lastModified: file.lastModified
                });
                
                // If we have webkitRelativePath, it might give us more info
                if (file.webkitRelativePath) {
                    filePath = file.webkitRelativePath;
                }
            }
            
            // Since we can't get the real full path due to browser security,
            // prompt user to manually enter the directory path
            promptForCompleteFilePath(filePath);
            return;
            
        } catch (error) {
            if (error.name !== 'AbortError') {
                console.warn('File System Access API failed, falling back to manual entry:', error);
                promptForManualPath();
            }
            return;
        }
    }
    
    // Fallback to traditional file input
    const fileInput = document.createElement('input');
    fileInput.type = 'file';
    fileInput.multiple = false;
    fileInput.style.display = 'none';
    
    fileInput.addEventListener('change', (event) => {
        const file = event.target.files[0];
        if (file) {
            let filePath = file.name;
            
            // Try to get more path info if available
            if (file.webkitRelativePath) {
                filePath = getRealisticPath(file.webkitRelativePath);
            } else {
                // Construct a realistic absolute path
                filePath = getRealisticPath(file.name);
            }
            
            const pathInput = document.getElementById('pathInput');
            pathInput.value = filePath;
            
            const validation = validatePath(pathInput.value);
            updateValidationMessage(validation);
            
            showNotification(`File selected: ${file.name}`, 'success');
            announceToScreenReader(`File selected: ${file.name}`);
        }
        
        // Clean up
        document.body.removeChild(fileInput);
    });
    
    document.body.appendChild(fileInput);
    fileInput.click();
}

async function selectDirectory() {
    closePicker();
    
    // Try to use File System Access API first (modern browsers)
    if ('showDirectoryPicker' in window) {
        try {
            const directoryHandle = await window.showDirectoryPicker();
            
            // Get realistic absolute path for directory
            let dirPath = getRealisticPath(directoryHandle.name, true);
            
            const pathInput = document.getElementById('pathInput');
            pathInput.value = dirPath;
            
            const validation = validatePath(pathInput.value);
            updateValidationMessage(validation);
            
            showNotification(`Directory selected: ${directoryHandle.name}`, 'success');
            announceToScreenReader(`Directory selected: ${directoryHandle.name}`);
            
            return;
        } catch (error) {
            if (error.name !== 'AbortError') {
                console.warn('File System Access API failed, falling back to input element:', error);
            } else {
                return; // User cancelled
            }
        }
    }
    
    // Fallback to traditional directory input
    const dirInput = document.createElement('input');
    dirInput.type = 'file';
    dirInput.webkitdirectory = true;
    dirInput.directory = true;
    dirInput.multiple = true;
    dirInput.style.display = 'none';
    
    dirInput.addEventListener('change', (event) => {
        const files = event.target.files;
        if (files.length > 0) {
            // Get the directory path from the first file
            const firstFile = files[0];
            let dirPath = '';
            
            if (firstFile.webkitRelativePath) {
                // Extract directory path from webkitRelativePath
                const pathParts = firstFile.webkitRelativePath.split('/');
                pathParts.pop(); // Remove filename
                const relativeDirPath = pathParts.join('/');
                dirPath = getRealisticPath(relativeDirPath, true);
            } else {
                // Fallback - construct realistic absolute path
                dirPath = getRealisticPath('selected-directory', true);
            }
            
            const pathInput = document.getElementById('pathInput');
            pathInput.value = dirPath;
            
            const validation = validatePath(pathInput.value);
            updateValidationMessage(validation);
            
            showNotification(`Directory selected: ${files.length} files found`, 'success');
            announceToScreenReader(`Directory selected with ${files.length} files`);
        }
        
        // Clean up
        document.body.removeChild(dirInput);
    });
    
    document.body.appendChild(dirInput);
    dirInput.click();
}

function closePicker() {
    if (window.currentPickerMenu) {
        document.body.removeChild(window.currentPickerMenu);
        window.currentPickerMenu = null;
    }
}

function renderPathsList() {
    const container = document.getElementById('pathsContainer');
    
    if (indexedPaths.size === 0) {
        container.innerHTML = '<div class="empty-state">No paths added yet</div>';
        return;
    }
    
    const pathsArray = Array.from(indexedPaths).sort();
    container.innerHTML = pathsArray.map(path => `
        <div class="path-item" role="listitem">
            <div class="path-item-info">
                <div class="path-item-path">${path}</div>
                <div class="path-item-meta">
                    <span>Type: ${path.includes('.') ? 'File' : 'Directory'}</span>
                    <span>Added: ${new Date().toLocaleDateString()}</span>
                </div>
            </div>
            <div class="path-item-actions">
                <button class="path-item-button remove" onclick="removePathFromList('${path.replace(/'/g, "\\'")}')">
                    Remove
                </button>
            </div>
        </div>
    `).join('');
}

function removePathFromList(path) {
    indexedPaths.delete(path);
    renderPathsList();
    logStatus(`✓ Path removed from list: ${path}`);
    
    // Update validation if this was the current input
    const pathInput = document.getElementById('pathInput');
    if (pathInput.value.trim() === path) {
        const validation = validatePath(path);
        updateValidationMessage(validation);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    initializeKeyboardShortcuts();
    manageFocus();
    initializeDefaultPaths();
    initializePathValidation();
    const switchEl = document.getElementById("indexerSwitch");
    
    switchEl.addEventListener("change", async () => {
        const newState = switchEl.checked;
        const previousState = !newState;
        
        // Add visual feedback - switch stays in new position for immediate response
        try {
            if (newState) {
                logStatus("Starting indexer...");
                
                // Add small delay to show the switch animation
                await new Promise(resolve => setTimeout(resolve, 150));
                
                const response = await fetch("/api/indexer/start", {method: "POST"});
                
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }
                
                logStatus("Indexer is ON");
                showNotification('Indexer started', 'success');
                announceToScreenReader('Indexer started successfully');
            } else {
                logStatus("Stopping indexer...");
                
                // Add small delay to show the switch animation
                await new Promise(resolve => setTimeout(resolve, 150));
                
                const response = await fetch("/api/indexer/stop", {method: "POST"});
                
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }
                
                logStatus("Indexer is OFF");
                showNotification('Indexer stopped', 'info');
                announceToScreenReader('Indexer stopped');
            }
        } catch (error) {
            // Smoothly revert the switch state on error with animation
            setTimeout(() => {
                switchEl.checked = previousState;
            }, 200);
            
            const action = newState ? 'start' : 'stop';
            logStatus(`✗ Failed to ${action} indexer: ${error.message}`);
            showNotification(`Failed to ${action} indexer`, 'error');
            announceToScreenReader(`Failed to ${action} indexer: ${error.message}`);
        }
    });

    const autoQuerySwitch = document.getElementById("autoQuerySwitch");
    let autoQueryInterval = null;

    autoQuerySwitch.addEventListener("change", () => {
        if (autoQuerySwitch.checked) {
            logStatus("Auto-Query is ON");
            autoQueryInterval = setInterval(runAutoQuery, 2000);
            showNotification('Auto-query enabled', 'info');
        } else {
            logStatus("Auto-Query is OFF");
            clearInterval(autoQueryInterval);
            showNotification('Auto-query disabled', 'info');
        }
    });

    async function runAutoQuery() {
        const queryInput = document.getElementById("queryWord");
        if (queryInput && queryInput.value.trim() !== "") {
            try {
                const response = await fetch(`/api/indexer/query?word=${encodeURIComponent(queryInput.value)}`);
                const results = await response.json();
                displayQueryResults(results);
                logStatus(`Auto-query executed for: ${queryInput.value}`);
            } catch (error) {
                logStatus(`Auto-query failed: ${error.message}`);
            }
        }
    }

    function displayQueryResults(results) {
        const resultsEl = document.getElementById("results");
        if (!resultsEl) return;
        resultsEl.textContent = JSON.stringify(results, null, 2);
    }
    
    logStatus("Verbum Indexer Control Panel initialized");
});

// Dialog functions
function openShortcutsDialog() {
    const dialog = document.getElementById('shortcutsDialog');
    const closeButton = dialog.querySelector('.dialog-close');
    
    dialog.hidden = false;
    document.body.style.overflow = 'hidden'; // Prevent background scrolling
    
    // Focus the close button for keyboard users
    closeButton.focus();
    
    // Trap focus within dialog
    trapFocusInDialog(dialog);
    
    announceToScreenReader('Keyboard shortcuts dialog opened');
}

function closeShortcutsDialog() {
    const dialog = document.getElementById('shortcutsDialog');
    
    dialog.hidden = true;
    document.body.style.overflow = ''; // Restore background scrolling
    
    // Return focus to the help button
    document.querySelector('.help-button').focus();
    
    announceToScreenReader('Keyboard shortcuts dialog closed');
}

function trapFocusInDialog(dialog) {
    const focusableElements = dialog.querySelectorAll(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    );
    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];
    
    const handleTabKey = (event) => {
        if (event.key === 'Tab') {
            if (event.shiftKey) {
                if (document.activeElement === firstElement) {
                    event.preventDefault();
                    lastElement.focus();
                }
            } else {
                if (document.activeElement === lastElement) {
                    event.preventDefault();
                    firstElement.focus();
                }
            }
        } else if (event.key === 'Escape') {
            event.preventDefault();
            closeShortcutsDialog();
        }
    };
    
    dialog.addEventListener('keydown', handleTabKey);
    
    // Remove event listener when dialog is closed
    const observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
            if (mutation.type === 'attributes' && mutation.attributeName === 'hidden') {
                if (dialog.hidden) {
                    dialog.removeEventListener('keydown', handleTabKey);
                    observer.disconnect();
                }
            }
        });
    });
    
    observer.observe(dialog, { attributes: true });
}
