// File: src/main/resources/public/js/login.js
document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("loginForm");
    const loginSpinner = document.getElementById("loginSpinner");

    // Get references to the modal elements
    const myAlertModal = new bootstrap.Modal(document.getElementById('myAlertModal'));
    const myAlertModalLabel = document.getElementById('myAlertModalLabel');
    const myAlertModalBody = document.getElementById('myAlertModalBody');

    loginForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const email = document.getElementById("email").value;
        const password = document.getElementById("password").value;

        loginSpinner.classList.remove("d-none");

        try {
            const response = await fetch("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email : email, password : password })
            });

            // Always attempt to parse JSON.
            const data = await response.json();

            // Check response.ok for success (2xx status codes)
            if (response.ok) { // This handles status 200 explicitly
                // Your Java backend for successful login sets `success: true`
                // and includes the user object.
                showAlert('success', 'Login exitoso!'); // Use the new showAlert function
                // Optional: You might want to delay the redirect until the user closes the modal
                // Or handle the redirect directly after showing the success message, depending on UX
                myAlertModal._element.addEventListener('hidden.bs.modal', function () {
                    window.location.href = '/';
                }, { once: true }); // Use { once: true } to remove the listener after it fires
            } else {
                // For non-2xx status codes (400, 401, 500),
                // your Java backend returns `success: false` and `message`
                showAlert('danger', data.message || 'Error en el login. Por favor intenta nuevamente.'); // Use the new showAlert function
            }
        } catch (error) {
            // This catch block is for network errors or if response.json() fails
            console.error('Error:', error);
            showAlert('danger', 'Error de conexión. Por favor intenta nuevamente.'); // Use the new showAlert function
        } finally {
            loginSpinner.classList.add("d-none");
        }
    });

    /**
     * Displays a Bootstrap modal alert.
     * @param {string} type - The type of alert (e.g., 'success', 'danger', 'info', 'warning'). This affects the modal title.
     * @param {string} message - The message to display in the alert.
     */
    function showAlert(type, message, duration = 1000) {
        // Set modal title based on type
        switch (type) {
            case 'success':
                myAlertModalLabel.textContent = 'Éxito';
                myAlertModalLabel.parentElement.className = 'modal-header bg-success text-white';
                break;
            case 'danger':
                myAlertModalLabel.textContent = 'Error';
                myAlertModalLabel.parentElement.className = 'modal-header bg-danger text-white';
                break;
            case 'info':
                myAlertModalLabel.textContent = 'Información';
                myAlertModalLabel.parentElement.className = 'modal-header bg-info text-white';
                break;
            case 'warning':
                myAlertModalLabel.textContent = 'Advertencia';
                myAlertModalLabel.parentElement.className = 'modal-header bg-warning text-dark';
                break;
            default:
                myAlertModalLabel.textContent = 'Alerta';
                myAlertModalLabel.parentElement.className = 'modal-header';
        }

        // Set modal body message
        myAlertModalBody.textContent = message;

        // Show the modal
        myAlertModal.show();

        // Set timeout to hide the modal after duration (default: 3000ms = 3 seconds)
        setTimeout(() => {
            myAlertModal.hide();
        }, duration);
    }
});