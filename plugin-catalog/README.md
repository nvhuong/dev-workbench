# Official Plugin Catalog

This directory stores the official online plugin catalogs consumed by DevWorkbench.

- `catalog-github.json`: official GitHub source

These files are treated as generated release metadata.

Do not edit them casually by hand. The current primary update path is:

- `.github/workflows/plugin-release.yml`

That workflow can update:

- public catalogs in `plugin-catalog/`
- bundled fallback catalogs in `dev-workbench-plugins/plugin-manager/src/main/resources/plugin-catalog/`

Current behavior:

- each plugin release may update the matching GitHub catalog entry
- bundled fallback catalogs are kept in sync with the public catalogs
- `sha256` is supported by the installer and should be filled by the release pipeline when available
- official plugin releases are expected to run from the default branch
- the release workflow validates tag / pom / descriptor / catalog consistency before publishing

If you need to adjust catalog structure manually, update both places together:

- `plugin-catalog/`
- `dev-workbench-plugins/plugin-manager/src/main/resources/plugin-catalog/`
