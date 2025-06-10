// File: src/main/resources/public/js/register.js
document.addEventListener('DOMContentLoaded', function() {
    const registerForm = document.getElementById('registerForm');
    const registerSpinner = document.getElementById('registerSpinner');

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
            alert('Las contraseñas no coinciden');
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
                alert('Registro exitoso! Por favor inicia sesión.');
                window.location.href = 'login.html';
            } else {
                alert(data.error || 'Error en el registro. Por favor intenta nuevamente.');
            }
        } catch (error) {
            console.error('Error:', error);
            alert('Error de conexión. Por favor intenta nuevamente.');
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
});