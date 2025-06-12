// File: src/main/resources/public/js/builder.js
document.addEventListener('DOMContentLoaded', () => {
    // --- State Variables ---
    let allMaterials = [];
    let selectedMaterials = [];

    // --- DOM Elements ---
    const materialsList = document.getElementById('materialsList');
    const materialsSpinner = document.getElementById('materialsSpinner');
    const materialSearch = document.getElementById('materialSearch');
    const braceletPreview = document.getElementById('braceletPreview');
    const previewPlaceholder = document.getElementById('previewPlaceholder');
    const totalPriceEl = document.getElementById('totalPrice');
    const saveBuildForm = document.getElementById('saveBuildForm');
    const braceletNameInput = document.getElementById('braceletName');
    const saveSpinner = document.getElementById('saveSpinner');

    // --- Modals ---
    const myAlertModal = new bootstrap.Modal(document.getElementById('myAlertModal'));
    const myAlertModalLabel = document.getElementById('myAlertModalLabel');
    const myAlertModalBody = document.getElementById('myAlertModalBody');

    // --- Initial Setup ---
    checkAuthAndLoad();

    // --- Event Listeners ---
    materialSearch.addEventListener('input', renderMaterials);
    saveBuildForm.addEventListener('submit', saveBuild);

    // --- Core Functions ---

    /**
     * Checks if the user is authenticated. If so, loads materials.
     * If not, redirects to the login page.
     */
    async function checkAuthAndLoad() {
        try {
            const response = await fetch("/api/auth/status");
            const data = await response.json();

            if (data.authenticated) {
                updateHeader(data);
                loadMaterials(); // User is logged in, proceed to load materials
            } else {
                // If not authenticated, redirect to login
                window.location.href = '/login.html';
            }
        } catch (error) {
            console.error("Error checking auth status:", error);
            // Redirect to login on error as well, as we can't verify the user
            window.location.href = '/login.html';
        }
    }

    /**
     * Fetches the list of available materials from the backend.
     */
    async function loadMaterials() {
        materialsSpinner.style.display = 'flex';
        try {
            // NOTE: Using the public endpoint you already established in admin.js
            const response = await fetch('/api/public/materials');
            if (!response.ok) {
                throw new Error('Could not fetch materials.');
            }
            allMaterials = await response.json();
            renderMaterials();
        } catch (error) {
            console.error('Error loading materials:', error);
            materialsList.innerHTML = `<div class="alert alert-danger">Error al cargar materiales.</div>`;
        } finally {
            materialsSpinner.style.display = 'none';
        }
    }

    /**
     * Renders materials in the list, applying the search filter.
     */
    function renderMaterials() {
        materialsList.innerHTML = '';
        const searchTerm = materialSearch.value.toLowerCase();

        const filteredMaterials = allMaterials.filter(material =>
            material.nombre.toLowerCase().includes(searchTerm) && material.cantidadInventario > 0
        );

        if (filteredMaterials.length === 0) {
            materialsList.innerHTML = '<p class="text-center text-muted">No se encontraron materiales.</p>';
            return;
        }

        filteredMaterials.forEach(material => {
            const item = document.createElement('div');
            item.className = 'material-item';
            // Assuming each material has a price. If not, you'll need to add it to your Material model.
            // For now, I'll use a placeholder price logic.
            const materialPrice = material.price || (material.tamanoMm * 2.5); // Example pricing
            item.innerHTML = `
                <span>
                    <i class="bi bi-gem"></i> ${material.nombre} (${material.tamanoMm}mm)
                </span>
                <strong>$${materialPrice.toFixed(2)}</strong>
            `;
            item.dataset.id = material.id;
            item.dataset.price = materialPrice;
            item.dataset.name = material.nombre;

            item.addEventListener('click', () => toggleMaterialSelection(material.id, materialPrice, material.nombre));
            materialsList.appendChild(item);
        });
    }

    /**
     * Adds or removes a material from the user's selection.
     */
    function toggleMaterialSelection(materialId, price, name) {
        const index = selectedMaterials.findIndex(m => m.id === materialId);
        if (index > -1) {
            // Remove material (optional, if you want users to be able to remove)
            // selectedMaterials.splice(index, 1);
        } else {
            // Add material
            selectedMaterials.push({ id: materialId, price, name });
        }
        updatePreview();
    }

    /**
     * Updates the preview pane with selected materials and the total price.
     */
    function updatePreview() {
        if (selectedMaterials.length === 0) {
            previewPlaceholder.classList.remove('d-none');
            braceletPreview.innerHTML = ''; // Clear any existing items
            braceletPreview.appendChild(previewPlaceholder);
            totalPriceEl.textContent = '$0.00';
            return;
        }

        previewPlaceholder.classList.add('d-none');
        braceletPreview.innerHTML = '';
        let totalPrice = 0;

        selectedMaterials.forEach(material => {
            const item = document.createElement('div');
            item.className = 'preview-item';
            item.innerHTML = `
                <span>${material.name}</span>
                <span>$${material.price.toFixed(2)}</span>
            `;
            braceletPreview.appendChild(item);
            totalPrice += material.price;
        });

        totalPriceEl.textContent = `$${totalPrice.toFixed(2)}`;
    }

    /**
     * Handles the submission of the new bracelet design.
     */
    async function saveBuild(event) {
        event.preventDefault();
        const braceletName = braceletNameInput.value.trim();
        const materialIds = selectedMaterials.map(m => m.id);

        if (!braceletName || materialIds.length === 0) {
            showAlert('warning', 'Por favor, dale un nombre a tu pulsera y selecciona al menos un material.');
            return;
        }

        saveSpinner.classList.remove('d-none');

        try {
            const response = await fetch('/api/usuario/pulseras/design', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    nombre: braceletName,
                    materialesIds: materialIds
                })
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error || 'No se pudo guardar el diseño.');
            }

            const result = await response.json();
            showAlert('success', '¡Tu diseño se ha guardado con éxito!');

            // Redirect to account page after modal closes
            myAlertModal._element.addEventListener('hidden.bs.modal', () => {
                window.location.href = '/account.html';
            }, { once: true });

        } catch (error) {
            showAlert('danger', `Error: ${error.message}`);
        } finally {
            saveSpinner.classList.add('d-none');
        }
    }


    // --- Helper Functions ---
    /**
     * Updates header based on authentication status.
     * Reused from your other scripts.
     */
    function updateHeader(data) {
        const guestMenu = document.getElementById("guestMenu");
        const userMenu = document.getElementById("userMenu");
        const adminPanelLink = userMenu.querySelector('a[href="/admin"]');

        if (data.authenticated) {
            guestMenu.classList.add("d-none");
            userMenu.classList.remove("d-none");

            const dropdownToggle = userMenu.querySelector(".dropdown-toggle");
            if (data.username) {
                dropdownToggle.innerHTML = `<i class="bi bi-person-circle"></i> ${data.username}`;
            }

            if (adminPanelLink) {
                if (data.role === 'admin') {
                    adminPanelLink.closest('li').classList.remove("d-none");
                } else {
                    adminPanelLink.closest('li').classList.add("d-none");
                }
            }
        } else {
            guestMenu.classList.remove("d-none");
            userMenu.classList.add("d-none");
        }
    }

    /**
     * Displays a Bootstrap modal alert.
     * Reused from your other scripts.
     */
    function showAlert(type, message, duration = 2000) {
        myAlertModalLabel.textContent = type.charAt(0).toUpperCase() + type.slice(1);
        myAlertModalBody.textContent = message;

        const header = myAlertModal._element.querySelector('.modal-header');
        header.className = 'modal-header'; // Reset classes
        if (type === 'success') header.classList.add('bg-success', 'text-white');
        else if (type === 'danger') header.classList.add('bg-danger', 'text-white');
        else if (type === 'warning') header.classList.add('bg-warning', 'text-dark');
        else if (type === 'info') header.classList.add('bg-info', 'text-white');

        myAlertModal.show();
        setTimeout(() => myAlertModal.hide(), duration);
    }
});