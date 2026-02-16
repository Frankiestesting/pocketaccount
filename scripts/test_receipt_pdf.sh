#!/usr/bin/env bash
set -euo pipefail

./mvnw -q -Dtest=com.frnholding.pocketaccount.receipt.ReceiptPdfSmokeTest test
