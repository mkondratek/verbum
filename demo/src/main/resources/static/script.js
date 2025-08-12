async function queryIndexer() {
    const word = document.getElementById('queryWord').value.trim();
    if (!word) {
        showNotification('Please enter a word to query', 'warning');
        return;
    }
    
    try {
        const res = await fetch(`/api/indexer/query?word=${encodeURIComponent(word)}`);
        const data = await res.json();
        document.getElementById('results').textContent = JSON.stringify(data, null, 2);
        showNotification('Query completed successfully', 'success');
    } catch (error) {
        showNotification('Failed to query indexer', 'error');
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

document.addEventListener("DOMContentLoaded", () => {
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
