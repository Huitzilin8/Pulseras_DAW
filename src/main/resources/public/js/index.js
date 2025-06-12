// File: src/main/resources/public/js/main.js

document.addEventListener("DOMContentLoaded", () => {

    // Get references to the modal elements
    const myAlertModal = new bootstrap.Modal(document.getElementById('myAlertModal'));
    const myAlertModalLabel = document.getElementById('myAlertModalLabel');
    const myAlertModalBody = document.getElementById('myAlertModalBody');

    // Elementos del DOM
    const galleryContainer = document.getElementById("galleryContainer");
    const searchInput = document.getElementById("searchInput");
    const searchBtn = document.getElementById("searchBtn");
    const userDropdown = document.querySelector(".dropdown-toggle");

    // Estado de la aplicación
    let allBracelets = [];
    let filteredBracelets = [];

    // Cargar pulseras al inici ar
    fetchBracelets();

    // Event listeners
    searchBtn.addEventListener("click", filterBracelets);
    searchInput.addEventListener("keyup", (e) => {
        if (e.key === "Enter") filterBracelets();
    });

    async function fetchBracelets() {
        try {
            // 1. Obtener todas las pulseras disponibles (públicas)
            const braceletsResponse = await fetch("/api/public/pulseras");
            if (!braceletsResponse.ok) {
                // Si la respuesta no es OK, intenta leer el error del servidor
                const errorData = await braceletsResponse.json();
                throw new Error(errorData.error || "Error al cargar las pulseras.");
            }
            allBracelets = await braceletsResponse.json();

            // 2. Verificar el estado de autenticación del usuario
            const authStatusResponse = await fetch("/api/auth/status");
            if (authStatusResponse.ok) {
                const authStatus = await authStatusResponse.json();

                // Si el usuario está autenticado, obtener sus pulseras favoritas
                if (authStatus.authenticated) {
                    try {
                        const favoritesResponse = await fetch("/api/usuario/profile/favoritos");
                        if (!favoritesResponse.ok) {
                            // Si hay un error al cargar favoritos (ej. 401 si la sesión expiró), manejarlo.
                            const errorData = await favoritesResponse.json();
                            console.warn("Advertencia: No se pudieron cargar los favoritos del usuario:", errorData.error);
                            // No lanzamos un error fatal aquí para que las pulseras públicas se sigan mostrando
                            // pero sin el estado de favorito correcto si hubo un problema.
                        } else {
                            const favoriteBracelets = await favoritesResponse.json();
                            // Extraer solo los IDs de las pulseras favoritas para `markFavorites`
                            const favoriteIds = favoriteBracelets.map(b => b.id);
                            markFavorites(allBracelets, favoriteIds);
                        }
                    } catch (favError) {
                        console.warn("Advertencia: Error de red al cargar favoritos:", favError);
                        // Similar, no lanzamos un error fatal.
                    }
                }
            } else {
                console.warn("Advertencia: No se pudo verificar el estado de autenticación.");
                // Si no podemos verificar la autenticación, asumimos que no hay favoritos.
            }

            // 3. Filtrar y renderizar las pulseras
            filteredBracelets = [...allBracelets]; // Inicializar filteredBracelets con todas las pulseras
            renderBracelets(); // Llama a tu función para renderizar las pulseras en la UI

        } catch (error) {
            console.error("Error en fetchBracelets():", error);
            // Asegúrate de que `galleryContainer` esté definido en tu script
            if (galleryContainer) {
                galleryContainer.innerHTML = `
                    <div class="alert alert-danger col-12" role="alert">
                        Error al cargar las pulseras: ${error.message}
                    </div>`;
            }
        }
    }
    // Función para marcar las pulseras favoritas
    function markFavorites(bracelets, favoriteIds) {
        bracelets.forEach(bracelet => {
            bracelet.favorito = favoriteIds.some(id => id === bracelet.id);
        });
    }
        // Filtrar pulseras basado en el input de búsqueda
    function filterBracelets() {
        const searchTerm = searchInput.value.toLowerCase();
        filteredBracelets = allBracelets.filter(bracelet =>
            bracelet.nombre.toLowerCase().includes(searchTerm) ||
            bracelet.materialesIds.some(m => m.toLowerCase().includes(searchTerm))
        );

        renderBracelets();
    }

    // Renderizar las pulseras en la galería
    function renderBracelets() {
        if (filteredBracelets.length === 0) {
            galleryContainer.innerHTML = `
                <div class="col-12 text-center py-5">
                    <i class="bi bi-search" style="font-size: 3rem;"></i>
                    <h4 class="mt-3">No se encontraron pulseras</h4>
                </div>`;
            return;
        }

        // Wrap the mapped bracelets in a 'row' div
        galleryContainer.innerHTML = `<div class="row">` + filteredBracelets.map(bracelet => `
            <div class="col-md-4 col-lg-4 mb-4">
                <div class="card h-100">
                    <img src="${bracelet.imgURL}"
                         class="card-img-top" alt="${bracelet.nombre}">
                    <div class="card-body">
                        <h5 class="card-title">${bracelet.nombre}</h5>
                        <p class="card-text text-muted">
                            <small>${bracelet.materialesIds.join(", ")}</small>
                        </p>
                        <p class="card-text">$${bracelet.precio.toFixed(2)}</p>
                    </div>
                    <div class="card-footer bg-white border-top-0">
                        <button class="btn btn-sm btn-outline-primary toggle-favorite" data-id="${bracelet.id}">
                            <i class="bi ${bracelet.favorito ? 'bi-heart-fill text-danger' : 'bi-heart'}"></i>
                        </button>
                    </div>
                </div>
            </div>
        `).join("") + `</div>`; // Close the 'row' div

        // Añadir event listeners a los botones
        document.querySelectorAll(".toggle-favorite").forEach(btn => {
            btn.addEventListener("click", toggleFavorite);
        });
    }

    // Manejar favoritos
    async function toggleFavorite(e) {
        const braceletId = e.currentTarget.getAttribute("data-id");
        const iconElement = e.currentTarget.querySelector("i");
        const isFavorite = iconElement.classList.contains("bi-heart-fill");

        try {
            const response = await fetch(`/api/usuario/pulseras/${braceletId}/favorito`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                // El cuerpo no es estrictamente necesario para el backend que creamos,
                // ya que el backend simplemente "togglea" el estado.
                // body: JSON.stringify({ favorito: !isFavorite })
            });

            if (!response.ok) {
                const errorData = await response.json();
                console.error("Error del servidor al alternar favorito:", errorData);

                if (response.status === 401) {
                    showAlert('danger', 'No estás autenticado. Redirigiendo a login...',100000);
                    myAlertModal._element.addEventListener('hidden.bs.modal', function () {
                        window.location.href = '/login';
                    }, { once: true }); // Use { once: true } to remove the listener after it fires
                    return; // Detener la ejecución
                } else if (response.status === 404) {
                    showAlert('warning', 'La pulsera no se encontró.');
                } else {
                    showAlert('danger', errorData.error || "Error al actualizar el favorito.");
                }
                return; // Detener la ejecución si hay un error HTTP
            }

            // Si la respuesta es exitosa (response.ok es true)
            const responseData = await response.json(); // Para obtener el mensaje del backend
            console.log("Respuesta de favorito:", responseData);

            // Actualizar UI localmente sin recargar todo
            iconElement.classList.toggle("bi-heart");
            iconElement.classList.toggle("bi-heart-fill");
            iconElement.classList.toggle("text-danger");

            // Opcional: Actualizar el estado en tu array global `allBracelets`
            const bracelet = allBracelets.find(b => b.id === braceletId);
            if (bracelet) {
                bracelet.favorito = responseData.isFavorite; // Usa el valor retornado por el backend
            }

            // Mostrar alerta de éxito
            showAlert('success', responseData.message || "Favorito actualizado correctamente.");

        } catch (error) {
            console.error("Error en toggleFavorite():", error);
            showAlert('danger', "Error de conexión: " + error.message);
        }
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

    // Inicializar
    checkAuthStatus();
});