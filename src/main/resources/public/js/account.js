document.addEventListener('DOMContentLoaded', () => {
    // === DOM Element References ===
    const favoritesList = document.getElementById('favoritesList');
    const buildsList = document.getElementById('buildsList');
    const noFavoritesMessage = document.getElementById('noFavoritesMessage');
    const noBuildsMessage = document.getElementById('noBuildsMessage');
    const favoritesSpinner = document.getElementById('favoritesSpinner');
    const buildsSpinner = document.getElementById('buildsSpinner');

    // Modal elements for custom alerts
    const myAlertModal = new bootstrap.Modal(document.getElementById('myAlertModal'));
    const myAlertModalLabel = document.getElementById('myAlertModalLabel');
    const myAlertModalBody = document.getElementById('myAlertModalBody');

    // Form elements for username update
    const updateUsernameForm = document.getElementById('updateUsernameForm');
    const newUsernameInput = document.getElementById('newUsername');
    const usernameSpinner = document.getElementById('usernameSpinner');

    // Form elements for password update
    const updatePasswordForm = document.getElementById('updatePasswordForm');
    const currentPasswordInput = document.getElementById('currentPassword');
    const newPasswordInput = document.getElementById('newPassword');
    const confirmNewPasswordInput = document.getElementById('confirmNewPassword');
    const passwordSpinner = document.getElementById('passwordSpinner');

    // === Update Username Functionality ===
    updateUsernameForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        usernameSpinner.classList.remove('d-none'); // Show spinner
        const newUsername = newUsernameInput.value;

        try {
            const response = await fetch('/api/usuario/profile/username', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ newUsername })
            });

            const data = await response.json(); // Always try to parse JSON for more details

            if (response.ok) {
                showAlert('success', data.message || 'Nombre de usuario actualizado correctamente.');
                // Update username in the header dropdown if it exists
                const dropdownToggle = document.querySelector("#userMenu .dropdown-toggle");
                if (dropdownToggle) {
                    dropdownToggle.innerHTML = `<i class="bi bi-person-circle"></i> ${newUsername}`;
                }
                newUsernameInput.value = ''; // Clear input field
            } else {
                // Use error message from server, or a generic one
                showAlert('danger', data.error || 'Error desconocido al actualizar el nombre de usuario.');
            }
        } catch (error) {
            console.error('Error al actualizar el nombre de usuario:', error);
            showAlert('danger', 'Error de conexión. No se pudo actualizar el nombre de usuario.');
        } finally {
            usernameSpinner.classList.add('d-none'); // Hide spinner
        }
    });

    // === Update Password Functionality ===
    updatePasswordForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        passwordSpinner.classList.remove('d-none'); // Show spinner
        const currentPassword = currentPasswordInput.value;
        const newPassword = newPasswordInput.value;
        const confirmNewPassword = confirmNewPasswordInput.value;

        if (newPassword !== confirmNewPassword) {
            showAlert('warning', 'La nueva contraseña y su confirmación no coinciden.');
            passwordSpinner.classList.add('d-none'); // Hide spinner immediately
            return;
        }

        try {
            const response = await fetch('/api/usuario/profile/password', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ currentPassword, newPassword })
            });

            const data = await response.json(); // Always parse JSON for details

            if (response.ok) {
                showAlert('success', data.message || 'Contraseña actualizada correctamente.');
                // Clear all password fields on success
                currentPasswordInput.value = '';
                newPasswordInput.value = '';
                confirmNewPasswordInput.value = '';
            } else {
                showAlert('danger', data.error || 'Error desconocido al actualizar la contraseña.');
            }
        } catch (error) {
            console.error('Error al actualizar la contraseña:', error);
            showAlert('danger', 'Error de conexión. No se pudo actualizar la contraseña.');
        } finally {
            passwordSpinner.classList.add('d-none'); // Hide spinner
        }
    });

    // === Helper Function: Render Bracelet Item ===
    // This function now uses more detailed properties from the bracelet object.
    function renderBracelet(bracelet, listElement, type = 'favorite') {
        // Ensure we use 'id' and 'imgURL' from your API response
        const braceletId = bracelet.id;
        const braceletName = bracelet.nombre || 'Pulsera sin nombre';
        const braceletDescription = bracelet.descripcion ? `<p class="text-muted small mb-0">${bracelet.descripcion}</p>` : '';
        const braceletPrice = bracelet.precio ? `<span class="fw-bold">$${bracelet.precio.toFixed(2)}</span>` : '';
        const braceletImageUrl = bracelet.imgURL || 'https://via.placeholder.com/60'; // Use imgURL

        const itemHtml = `
            <div class="bracelet-item" data-id="${braceletId}">
                <div class="d-flex align-items-center">
                    <img src="${braceletImageUrl}" alt="${braceletName}" class="bracelet-image">
                    <div>
                        <h6 class="mb-0">${braceletName}</h6>
                        ${braceletDescription}
                        ${braceletPrice}
                    </div>
                </div>
                <div class="d-flex">
                    ${type === 'favorite' ? `
                        <button class="btn btn-danger btn-sm remove-favorite" data-id="${braceletId}" title="Quitar de favoritos">
                            <i class="bi bi-trash"></i>
                        </button>` : ''}
                    ${type === 'build' ? `
                        <a href="/builder?buildId=${braceletId}" class="btn btn-info btn-sm ms-2" title="Editar diseño">
                            <i class="bi bi-pencil"></i>
                        </a>` : ''}
                </div>
            </div>
        `;
        listElement.insertAdjacentHTML('beforeend', itemHtml);
    }

    // === Load Favorite Bracelets ===
    async function loadFavorites() {
        favoritesSpinner.classList.remove('d-none'); // Show spinner
        favoritesList.innerHTML = ''; // Clear existing items
        noFavoritesMessage.classList.add('d-none'); // Hide no favorites message initially

        try {
            const response = await fetch('/api/usuario/profile/favoritos');

            if (!response.ok) {
                // If not OK, try to parse JSON for server-provided error
                const errorData = await response.json().catch(() => ({ error: `HTTP error! status: ${response.status}` }));

                if (response.status === 401) {
                    showAlert('danger', errorData.error || 'No estás autenticado. Redirigiendo a login...');
                    setTimeout(() => { window.location.href = '/login'; }, 1500); // Redirect after alert
                    return; // Stop further execution
                }
                throw new Error(errorData.error || `Error al cargar favoritos: ${response.status}`);
            }

            const favorites = await response.json();
            console.log("Favoritos cargados:", favorites); // For debugging

            if (favorites.length === 0) {
                noFavoritesMessage.classList.remove('d-none'); // Show message if no favorites
            } else {
                favorites.forEach(bracelet => renderBracelet(bracelet, favoritesList, 'favorite'));
            }
        } catch (error) {
            console.error('Error al cargar favoritos:', error);
            showAlert('danger', `Hubo un problema al cargar tus pulseras favoritas: ${error.message}.`);
        } finally {
            favoritesSpinner.classList.add('d-none'); // Hide spinner
        }
    }

    // === Load Custom Built Bracelets ===
    async function loadBuilds() {
        buildsSpinner.classList.remove('d-none'); // Show spinner
        buildsList.innerHTML = ''; // Clear existing items
        noBuildsMessage.classList.add('d-none'); // Hide no builds message initially

        try {
            const response = await fetch('/api/usuario/profile/builds');

            if (!response.ok) {
                // If not OK, parse JSON for server error
                const errorData = await response.json().catch(() => ({ error: `HTTP error! status: ${response.status}` }));

                if (response.status === 401) {
                    showAlert('danger', errorData.error || 'No estás autenticado. Redirigiendo a login...');
                    setTimeout(() => { window.location.href = '/login'; }, 1500); // Redirect after alert
                    return; // Stop further execution
                }
                throw new Error(errorData.error || `Error al cargar diseños: ${response.status}`);
            }

            const builds = await response.json();
            console.log("Builds cargados:", builds); // For debugging

            if (builds.length === 0) {
                noBuildsMessage.classList.remove('d-none'); // Show message if no builds
            } else {
                builds.forEach(bracelet => renderBracelet(bracelet, buildsList, 'build'));
            }
        } catch (error) {
            console.error("Error en loadBuilds():", error);
            showAlert('danger', `Error al cargar tus diseños personalizados: ${error.message}.`);
        } finally {
            buildsSpinner.classList.add('d-none'); // Hide spinner
        }
    }

    // === Remove from Favorites Functionality ===
    favoritesList.addEventListener('click', async (e) => {
        // Check if the clicked element or its closest parent is the remove button
        const removeButton = e.target.closest('.remove-favorite');
        if (removeButton) {
            const pulseraId = removeButton.dataset.id;
            if (!confirm('¿Estás seguro de que quieres eliminar esta pulsera de tus favoritos?')) {
                return; // User cancelled
            }

            try {
                const response = await fetch(`api/usuario/pulseras/${pulseraId}/favorito`, {
                    method: 'PUT'
                });

                if (response.status === 200) { // 204 No Content is standard for successful DELETE
                    showAlert('success', 'Pulsera eliminada de favoritos correctamente.');
                    loadFavorites(); // Reload the list to reflect the change
                } else if (!response.ok) {
                    const errorData = await response.json(); // Try to get server error message
                    throw new Error(errorData.error || `Failed to remove from favorites: ${response.status}`);
                }
            } catch (error) {
                console.error('Error al eliminar de favoritos:', error);
                showAlert('danger', `Error al eliminar de favoritos: ${error.message}.`);
            }
        }
    });

    // === Authentication Status Check (for Navbar) ===
    function checkAuthStatus() {
        fetch("/api/auth/status")
            .then(response => {
                if (!response.ok) throw new Error("Error checking auth status");
                return response.json();
            })
            .then(data => {
                const guestMenu = document.getElementById("guestMenu");
                const userMenu = document.getElementById("userMenu");
                const adminPanelLink = userMenu.querySelector('a[href="/admin"]');

                if (data.authenticated) {
                    // User is authenticated, display user menu
                    guestMenu.classList.add("d-none");
                    userMenu.classList.remove("d-none");

                    const dropdownToggle = userMenu.querySelector(".dropdown-toggle");
                    if (data.username) {
                        dropdownToggle.innerHTML = `<i class="bi bi-person-circle"></i> ${data.username}`;
                    }

                    if (adminPanelLink) {
                        if (data.role === 'admin') {
                            adminPanelLink.classList.remove("d-none");
                            adminPanelLink.closest('li').classList.remove("d-none");
                        } else {
                            adminPanelLink.classList.add("d-none");
                            adminPanelLink.closest('li').classList.add("d-none");
                        }
                    }
                } else {
                    // User is NOT authenticated, display guest menu and redirect
                    guestMenu.classList.remove("d-none");
                    userMenu.classList.add("d-none");

                    // Show alert and redirect
                    showAlert('danger', 'Sesión no válida. Por favor, inicia sesión para continuar.', 2000); // 2 seconds duration
                    setTimeout(() => {
                        window.location.href = '/login';
                    }, 2000); // Redirect after the alert duration
                }
            })
            .catch(error => {
                console.error("Error checking authentication status:", error);
                // Fallback to guest menu if there's an error from the fetch itself
                document.getElementById("guestMenu").classList.remove("d-none");
                document.getElementById("userMenu").classList.add("d-none");

                // Also show an alert for connection/server errors
                showAlert('danger', 'Error de conexión al verificar la sesión. Redirigiendo a login.', 2000);
                setTimeout(() => {
                    window.location.href = '/login';
                }, 2000);
            });
    }

    // === Custom Alert Modal Function ===
    function showAlert(type, message, duration = 3000) { // Default duration 3 seconds
        // Set modal title and header styling based on alert type
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

        myAlertModalBody.textContent = message; // Set the message in the modal body
        myAlertModal.show(); // Show the modal

        // Auto-hide the modal after a specified duration
        setTimeout(() => {
            myAlertModal.hide();
        }, duration);
    }

    // === Initial Load and Setup ===
    checkAuthStatus(); // Check authentication status for the navbar
    loadFavorites();   // Load user's favorite bracelets
    loadBuilds();      // Load user's custom built bracelets
});