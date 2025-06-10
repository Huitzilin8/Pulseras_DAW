// File: src/main/resources/public/account.js
document.addEventListener('DOMContentLoaded', () => {
    const favoritesList = document.getElementById('favoritesList');
    const buildsList = document.getElementById('buildsList');
    const noFavoritesMessage = document.getElementById('noFavoritesMessage');
    const noBuildsMessage = document.getElementById('noBuildsMessage');
    const favoritesSpinner = document.getElementById('favoritesSpinner');
    const buildsSpinner = document.getElementById('buildsSpinner');

    // Get references to the modal elements
    const myAlertModal = new bootstrap.Modal(document.getElementById('myAlertModal'));
    const myAlertModalLabel = document.getElementById('myAlertModalLabel');
    const myAlertModalBody = document.getElementById('myAlertModalBody');

    // Get references to the new forms and spinners
    const updateUsernameForm = document.getElementById('updateUsernameForm');
    const newUsernameInput = document.getElementById('newUsername');
    const usernameSpinner = document.getElementById('usernameSpinner');

    const updatePasswordForm = document.getElementById('updatePasswordForm');
    const currentPasswordInput = document.getElementById('currentPassword');
    const newPasswordInput = document.getElementById('newPassword');
    const confirmNewPasswordInput = document.getElementById('confirmNewPassword');
    const passwordSpinner = document.getElementById('passwordSpinner');

    // --- Update Username ---
        updateUsernameForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            usernameSpinner.classList.remove('d-none');
            const newUsername = newUsernameInput.value;

            try {
                const response = await fetch('/api/usuario/profile/username', {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ newUsername })
                });

                const data = await response.json();

                if (response.ok) {
                    showAlert('success', data.message);
                    // Optionally update the username in the header without full page reload
                    const dropdownToggle = document.querySelector(".dropdown-toggle");
                    if (dropdownToggle) {
                        dropdownToggle.innerHTML = `<i class="bi bi-person-circle"></i> ${newUsername}`;
                    }
                    newUsernameInput.value = ''; // Clear the input field
                } else {
                    showAlert('danger', data.error || 'Error al actualizar el nombre de usuario.');
                }
            } catch (error) {
                console.error('Error al actualizar el nombre de usuario:', error);
                showAlert('danger', 'Error de conexión al actualizar el nombre de usuario.');
            } finally {
                usernameSpinner.classList.add('d-none');
            }
        });

        // --- Update Password ---
        updatePasswordForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            passwordSpinner.classList.remove('d-none');
            const currentPassword = currentPasswordInput.value;
            const newPassword = newPasswordInput.value;
            const confirmNewPassword = confirmNewPasswordInput.value;

            if (newPassword !== confirmNewPassword) {
                showAlert('warning', 'La nueva contraseña y su confirmación no coinciden.');
                passwordSpinner.classList.add('d-none');
                return;
            }

            try {
                const response = await fetch('/api/usuario/profile/password', {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ currentPassword, newPassword })
                });

                const data = await response.json();

                if (response.ok) {
                    showAlert('success', data.message);
                    currentPasswordInput.value = '';
                    newPasswordInput.value = '';
                    confirmNewPasswordInput.value = ''; // Clear all input fields
                } else {
                    showAlert('danger', data.error || 'Error al actualizar la contraseña.');
                }
            } catch (error) {
                console.error('Error al actualizar la contraseña:', error);
                showAlert('danger', 'Error de conexión al actualizar la contraseña.');
            } finally {
                passwordSpinner.classList.add('d-none');
            }
        });

    // Function to display a bracelet item
    function renderBracelet(bracelet, listElement, type = 'favorite') {
        const itemHtml = `
            <div class="bracelet-item" data-id="${bracelet._id}">
                <div>
                    <img src="${bracelet.imageUrl || 'https://via.placeholder.com/60'}" alt="${bracelet.name}" class="bracelet-image">
                    <span>${bracelet.name || 'Pulsera sin nombre'}</span>
                </div>
                ${type === 'favorite' ? `<button class="btn btn-danger btn-sm remove-favorite" data-id="${bracelet._id}">
                    <i class="bi bi-trash"></i> Quitar
                </button>` : ''}
            </div>
        `;
        listElement.insertAdjacentHTML('beforeend', itemHtml);
    }

    // --- Load Favorite Bracelets ---
    async function loadFavorites() {
        favoritesSpinner.classList.remove('d-none');
        favoritesList.innerHTML = ''; // Clear previous items
        noFavoritesMessage.classList.add('d-none');

        try {

            const response = await fetch('/api/usuario/profile/favoritos');
            if (!response.ok) {
                if (response.status === 401) {
                    window.location.href = '/login'; // Redirect to login if unauthorized
                    return;
                }
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const favorites = await response.json();

            if (favorites.length === 0) {
                noFavoritesMessage.classList.remove('d-none');
                console.log('Prueba');
            } else {
                favorites.forEach(bracelet => renderBracelet(bracelet, favoritesList, 'favorite'));
            }
        } catch (error) {
            console.error('Error de conexión o inesperado al cargar favoritos:', error);
            showAlert('danger','Hubo un problema al cargar tus pulseras favoritas. Intenta de nuevo más tarde.');
        } finally {
            favoritesSpinner.classList.add('d-none');
        }
    }

    // --- Load Custom Built Bracelets ---
    async function loadBuilds() {
        buildsSpinner.classList.remove('d-none');
        buildsList.innerHTML = '';
        noBuildsMessage.classList.add('d-none');

        try {
            const response = await fetch('/api/usuario/profile/builds');

            // Si hay un error HTTP (401, 404, 500...)
            if (!response.ok) {
                const errorData = await response.json();
                console.error("Error del servidor:", errorData);

                if (response.status === 401) {
                    showAlert('danger','No estás autenticado. Redirigiendo a login...');
                    window.location.href = "/login";
                    return;
                }
                throw new Error(errorData.error || "Error al cargar builds");
            }

            const builds = await response.json();
            console.log("Builds cargados:", builds); // Depuración

            if (builds.length === 0) {
                noBuildsMessage.classList.remove('d-none');
            } else {
                builds.forEach(bracelet => renderBracelet(bracelet, buildsList, 'build'));
            }
        } catch (error) {
            console.error("Error en loadBuilds():", error);
            showAlert('danger',"Error al cargar diseños: " + error.message); // Mostrar el error real
        } finally {
            buildsSpinner.classList.add('d-none');
        }
    }
    // --- Add to Favorites (Example function, would be called from elsewhere) ---
    // You would typically call this from a product page, not from the account page directly
    async function addFavorite(pulseraId) {
        try {
            const response = await fetch('/api/usuario/profile/favoritos', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ pulseraId })
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error || 'Failed to add to favorites');
            }

            const data = await response.json();
            showAlert('info',data.message);
            loadFavorites(); // Reload the list to show the new favorite
        } catch (error) {
            console.error('Error adding to favorites:', error);
            showAlert('danger','Error al agregar a favoritos: ' + error.message);
        }
    }

    // --- Remove from Favorites ---
    favoritesList.addEventListener('click', async (e) => {
        if (e.target.classList.contains('remove-favorite') || e.target.closest('.remove-favorite')) {
            const button = e.target.closest('.remove-favorite');
            const pulseraId = button.dataset.id;
            if (!confirm('¿Estás seguro de que quieres eliminar esta pulsera de tus favoritos?')) {
                return;
            }

            try {
                const response = await fetch(`/api/usuario/profile/favoritos/${pulseraId}`, {
                    method: 'DELETE'
                });

                if (response.status === 204) { // 204 No Content for successful DELETE
                    showAlert('success','Pulsera eliminada de favoritos')
                    loadFavorites(); // Reload the list to reflect the change
                } else if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.error || 'Failed to remove from favorites');
                }
            } catch (error) {
                console.error('Error removing from favorites:', error);
                showAlert('danger','Error al eliminar de favoritos: ' + error.message);
            }
        }
    });

    // Verificar estado de autenticación
    function checkAuthStatus() {
        fetch("/api/auth/status")
            .then(response => {
                if (!response.ok) throw new Error("Error checking auth status");
                return response.json();
            })
            .then(data => {
                const guestMenu = document.getElementById("guestMenu");
                const userMenu = document.getElementById("userMenu");
                // Get the admin panel link from the user menu
                const adminPanelLink = userMenu.querySelector('a[href="/admin"]'); // Selects the <a> tag with href="/admin"
                console.log("Todo bien hasta ahora ...");
                if (data.authenticated) {
                    // Mostrar menú de usuario autenticado
                    guestMenu.classList.add("d-none");
                    userMenu.classList.remove("d-none");

                    // Actualizar nombre de usuario si está disponible
                    const dropdownToggle = userMenu.querySelector(".dropdown-toggle");
                    if (data.username) {
                        dropdownToggle.innerHTML = `<i class="bi bi-person-circle"></i> ${data.username}`;
                    }

                    // --- LOGIC FOR ADMIN PANEL ---
                    if (adminPanelLink) { // Ensure the link exists in the HTML
                        if (data.role === 'admin') { // Check if the user's role is 'admin'
                            adminPanelLink.classList.remove("d-none"); // Show the link
                            adminPanelLink.closest('li').classList.remove("d-none"); // Also show its parent <li> if you hid it
                        } else {
                            adminPanelLink.classList.add("d-none"); // Hide the link for non-admin users
                            adminPanelLink.closest('li').classList.add("d-none"); // Hide its parent <li>
                        }
                    }
                } else {
                    // Mostrar menú de invitado
                    guestMenu.classList.remove("d-none");
                    userMenu.classList.add("d-none");
                }
            })
            .catch(error => {
                console.error("Error:", error);
                // Por defecto mostrar menú de invitado si hay error
                document.getElementById("guestMenu").classList.remove("d-none");
                document.getElementById("userMenu").classList.add("d-none");
            });
    }

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

    // Inicializar
    checkAuthStatus();
    // Initial load of favorites and builds when the page loads
    loadFavorites();
    loadBuilds();

});
