// File: src/main/resources/public/js/main.js

document.addEventListener("DOMContentLoaded", () => {
    // Elementos del DOM
    const galleryContainer = document.getElementById("galleryContainer");
    const searchInput = document.getElementById("searchInput");
    const searchBtn = document.getElementById("searchBtn");
    const userDropdown = document.querySelector(".dropdown-toggle");

    // Estado de la aplicación
    let allBracelets = [];
    let filteredBracelets = [];

    // Cargar pulseras al iniciar
    fetchBracelets();

    // Event listeners
    searchBtn.addEventListener("click", filterBracelets);
    searchInput.addEventListener("keyup", (e) => {
        if (e.key === "Enter") filterBracelets();
    });

    // Obtener pulseras del backend
    function fetchBracelets() {
        fetch("/api/public/pulseras")
            .then(response => {
                if (!response.ok) throw new Error("Error al cargar las pulseras");
                return response.json();
            })
            .then(data => {
                allBracelets = data;
                filteredBracelets = [...allBracelets];
                renderBracelets();
            })
            .catch(error => {
                console.error("Error:", error);
                galleryContainer.innerHTML = `
                    <div class="alert alert-danger col-12">
                        Error al cargar las pulseras: ${error.message}
                    </div>`;
            });
    }

    // Filtrar pulseras basado en el input de búsqueda
    function filterBracelets() {
        const searchTerm = searchInput.value.toLowerCase();
        filteredBracelets = allBracelets.filter(bracelet =>
            bracelet.nombre.toLowerCase().includes(searchTerm) ||
            bracelet.materiales.some(m => m.toLowerCase().includes(searchTerm))
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

        galleryContainer.innerHTML = filteredBracelets.map(bracelet => `
            <div class="col-md-4 col-lg-3 mb-4">
                <div class="card h-100">
                    <img src="${bracelet.imagenUrl || 'https://via.placeholder.com/300x200?text=Pulsera'}"
                         class="card-img-top" alt="${bracelet.nombre}">
                    <div class="card-body">
                        <h5 class="card-title">${bracelet.nombre}</h5>
                        <p class="card-text text-muted">
                            <small>${bracelet.materiales.join(", ")}</small>
                        </p>
                        <p class="card-text">$${bracelet.precio.toFixed(2)}</p>
                    </div>
                    <div class="card-footer bg-white border-top-0">
                        <button class="btn btn-sm btn-outline-primary toggle-favorite" data-id="${bracelet.id}">
                            <i class="bi ${bracelet.favorito ? 'bi-heart-fill text-danger' : 'bi-heart'}"></i>
                        </button>
                        <button class="btn btn-sm btn-primary float-end add-to-cart" data-id="${bracelet.id}">
                            <i class="bi bi-cart-plus"></i> Añadir
                        </button>
                    </div>
                </div>
            </div>
        `).join("");

        // Añadir event listeners a los botones
        document.querySelectorAll(".toggle-favorite").forEach(btn => {
            btn.addEventListener("click", toggleFavorite);
        });

        document.querySelectorAll(".add-to-cart").forEach(btn => {
            btn.addEventListener("click", addToCart);
        });
    }

    // Manejar favoritos
    function toggleFavorite(e) {
        const braceletId = e.currentTarget.getAttribute("data-id");
        const isFavorite = e.currentTarget.querySelector("i").classList.contains("bi-heart-fill");

        fetch(`/api/pulseras/${braceletId}/favorito`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ favorito: !isFavorite })
        })
        .then(response => {
            if (response.ok) {
                // Actualizar UI localmente sin recargar todo
                const icon = e.currentTarget.querySelector("i");
                icon.classList.toggle("bi-heart");
                icon.classList.toggle("bi-heart-fill");
                icon.classList.toggle("text-danger");

                // Actualizar el estado
                const bracelet = allBracelets.find(b => b.id === braceletId);
                if (bracelet) bracelet.favorito = !isFavorite;
            }
        })
        .catch(error => console.error("Error:", error));
    }

    // Añadir al carrito
    function addToCart(e) {
        const braceletId = e.currentTarget.getAttribute("data-id");

        fetch("/api/carrito", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ pulseraId: braceletId, cantidad: 1 })
        })
        .then(response => {
            if (response.ok) {
                showAlert("success", "Pulsera añadida al carrito");
            } else {
                throw new Error("Error al añadir al carrito");
            }
        })
        .catch(error => {
            console.error("Error:", error);
            showAlert("danger", error.message);
        });
    }

    // Mostrar notificación
    function showAlert(type, message) {
        const alertDiv = document.createElement("div");
        alertDiv.className = `alert alert-${type} position-fixed top-0 end-0 m-3`;
        alertDiv.style.zIndex = "1000";
        alertDiv.innerHTML = message;

        document.body.appendChild(alertDiv);

        setTimeout(() => {
            alertDiv.classList.add("fade");
            setTimeout(() => alertDiv.remove(), 500);
        }, 3000);
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