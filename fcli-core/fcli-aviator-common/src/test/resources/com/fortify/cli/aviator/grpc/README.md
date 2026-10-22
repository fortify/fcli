The certificate and private key in this directory are public test fixtures for the
local TLS/CONNECT proxy test. They are not credentials for any deployed service.
The certificate covers `localhost` and `127.0.0.1`; the test client trusts only this
certificate. No production trust store is changed.
