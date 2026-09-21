# Security Policy

## Reporting a vulnerability

Please do not open a public issue for a suspected security vulnerability. Contact the maintainer, MRX7014, through the private contact options available on the [GitHub profile](https://github.com/mrx7014), and include reproduction steps, affected versions, and the potential impact.

## Supported versions

Security fixes are prioritized for the latest release on the `main` branch. Older releases may not receive fixes unless the issue has a significant impact.

## Secret handling

Signing keys, keystore files, passwords, tokens, and private certificates must never be committed. Release signing material belongs in GitHub Actions Secrets or an ignored local `keystore.properties` file. If a secret is accidentally exposed, revoke and rotate it immediately.
