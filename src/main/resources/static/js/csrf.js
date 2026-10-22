(function () {
    "use strict";

    const protectedMethods = new Set([
        "POST", "PUT", "PATCH", "DELETE"
    ]);

    const originalFetch = window.fetch.bind(window);

    function getCookie(name) {
        const prefix = name + "=";

        const cookie = document.cookie
            .split(";")
            .map(value => value.trim())
            .find(value => value.startsWith(prefix));

        if (!cookie) {
            return null;
        }

        const value = cookie.substring(prefix.length);

        try {
            return decodeURIComponent(value);
        } catch {
            return value;
        }
    }

    window.fetch = function (input, init = {}) {
        const requestMethod =
            init.method ||
            (input instanceof Request ? input.method : "GET");

        const method = String(requestMethod).toUpperCase();

        if (!protectedMethods.has(method)) {
            return originalFetch(input, init);
        }

        const token = getCookie("XSRF-TOKEN");

        const headers = new Headers(
            init.headers ||
            (input instanceof Request ? input.headers : undefined)
        );

        if (token) {
            headers.set("X-XSRF-TOKEN", token);
        }

        return originalFetch(input, {
            ...init,
            headers
        });
    };
})();