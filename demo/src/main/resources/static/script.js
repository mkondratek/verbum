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
    if (!path) {
        showNotification('Please enter a path to add', 'warning');
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
            logStatus(`✓ Path added successfully: ${path}`);
            showNotification('Path added successfully', 'success');
            document.getElementById('pathInput').value = '';
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

async function removePath() {
    const path = document.getElementById('pathInput').value.trim();
    if (!path) {
        showNotification('Please enter a path to remove', 'warning');
        return;
    }
    
    logStatus(`Removing path: ${path}`);
    
    try {
        const res = await fetch('/api/indexer/remove', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({path})
        });
        
        if (res.ok) {
            logStatus(`✓ Path removed successfully: ${path}`);
            showNotification('Path removed successfully', 'success');
            document.getElementById('pathInput').value = '';
        } else {
            const error = await res.text();
            logStatus(`✗ Failed to remove path: ${error}`);
            showNotification('Failed to remove path', 'error');
        }
    } catch (error) {
        logStatus(`✗ Network error while removing path: ${error.message}`);
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

document.addEventListener("DOMContentLoaded", () => {
    initializeKeyboardShortcuts();
    manageFocus();
    const switchEl = document.getElementById("indexerSwitch");
    
    switchEl.addEventListener("change", async () => {
        try {
            if (switchEl.checked) {
                logStatus("Starting indexer...");
                await fetch("/start", {method: "POST"});
                logStatus("Indexer is ON");
                showNotification('Indexer started', 'success');
            } else {
                logStatus("Stopping indexer...");
                await fetch("/stop", {method: "POST"});
                logStatus("Indexer is OFF");
                showNotification('Indexer stopped', 'info');
            }
        } catch (error) {
            logStatus(`Error: ${error.message}`);
            showNotification('Failed to toggle indexer', 'error');
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
