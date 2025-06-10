// File: src/main/resources/public/js/register.js
document.addEventListener('DOMContentLoaded', function() {
    const registerForm = document.getElementById('registerForm');
    const registerSpinner = document.getElementById('registerSpinner');

    // Get references to the modal elements
    const myAlertModal = new bootstrap.Modal(document.getElementById('myAlertModal'));
    const myAlertModalLabel = document.getElementById('myAlertModalLabel');
    const myAlertModalBody = document.getElementById('myAlertModalBody');

    registerForm.addEventListener('submit', async function(e) {
        e.preventDefault();

        // Show loading spinner
        registerSpinner.classList.remove('d-none');

        // Get form values
        const nombre = document.getElementById('nombre').value.trim();
        const email = document.getElementById('email').value.trim();
        const password = document.getElementById('password').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        // Validate password match
        if (password !== confirmPassword) {
            showAlert('warning','Las contraseñas no coinciden');
            registerSpinner.classList.add('d-none');
            return;
        }

        try {
            const response = await fetch('/api/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    nombre: nombre,
                    email: email,
                    password: password
                })
            });

            const data = await response.json();

            if (data.success) {
                showAlert('success','Registro exitoso! Por favor inicia sesión.');
                window.location.href = 'login.html';
            } else {
                showAlert('danger','Error en el registro: ' + data.error,2500);
            }
        } catch (error) {
            console.error('Error:', error);
            showAlert('warning','Error de conexión. Por favor intenta nuevamente.');
        } finally {
            registerSpinner.classList.add('d-none');
        }
    });

    // Basic password strength indicator
    const passwordInput = document.getElementById('password');
    passwordInput.addEventListener('input', function() {
        const strengthIndicator = document.createElement('div');
        strengthIndicator.className = 'form-text';

        if (passwordInput.value.length === 0) {
            return;
        } else if (passwordInput.value.length < 8) {
            strengthIndicator.textContent = 'Contraseña débil';
            strengthIndicator.style.color = 'red';
        } else if (passwordInput.value.length < 12) {
            strengthIndicator.textContent = 'Contraseña moderada';
            strengthIndicator.style.color = 'orange';
        } else {
            strengthIndicator.textContent = 'Contraseña fuerte';
            strengthIndicator.style.color = 'green';
        }

        // Update or create the indicator
        const existingIndicator = passwordInput.nextElementSibling;
        if (existingIndicator && existingIndicator.className === 'form-text') {
            existingIndicator.replaceWith(strengthIndicator);
        } else {
            passwordInput.parentNode.insertBefore(strengthIndicator, passwordInput.nextSibling);
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