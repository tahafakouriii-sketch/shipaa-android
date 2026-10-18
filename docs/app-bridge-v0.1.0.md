# Shipaa App Bridge — Integration milestone 0.1.0

The first installable WordPress plugin has been produced as `shipaa-app-bridge-v0.1.0.zip` for manual staging installation. This repo document tracks that deliverable; the ZIP has not been deployed to shipaa.ir.

## Current verified implementation
- GET `/wp-json/shipaa-app/v1/me`: logged-in user's ID, display name, first/last name, avatar only.
- GET `/wp-json/shipaa-app/v1/my-courses`: purchase-history products from completed or processing WooCommerce orders belonging to the currently logged-in user.
- Shortcode: `[shipaa_app_home]` for a Persian RTL test dashboard on the WordPress origin.
- Uses WordPress cookie authentication and REST nonce. No public personal-data endpoint.
- No student course-content permissions, national ID, phone, payment receipt or management action exposed.

## Staging test plan
1. Back up the site; install the delivered ZIP via Plugins > Add New > Upload Plugin on staging, if possible.
2. Activate and add `[shipaa_app_home]` to a private test page.
3. Test as logged-out user, enrolled user and admin.
4. Verify no third-party user data is returned by REST endpoints.
5. Confirm no interference with the existing dashboard, WooCommerce registration or Eitaa flow.
6. Compare actual manual-grant course access logic with order history; implement permission-authoritative course endpoints before streaming lessons.
7. Only afterward connect this to the web app UI and build an Android package.

## Security and release notes
- The GitHub repository was observed **public**; make it private before storing sensitive project materials.
- The ZIP is a test version only, not a fully connected Android app.
- The current UI returns to WordPress login; inline authentication is not yet implemented.
