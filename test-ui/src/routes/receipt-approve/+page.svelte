<script>
	import { onMount } from 'svelte';

	/**
	 * @typedef {Object} Receipt
	 * @property {string} id
	 * @property {string} documentId
	 * @property {string} [purchaseDate]
	 * @property {number} [totalAmount]
	 * @property {string} [currency]
	 * @property {string} [description]
	 * @property {string} [merchant]
	 */
	/**
	 * @typedef {Object} ReceiptMatch
	 * @property {string} id
	 * @property {string} bankTransactionId
	 * @property {number} matchedAmount
	 */
	/**
	 * @typedef {Object} MatchCandidate
	 * @property {string} bankTransactionId
	 * @property {string} bookingDate
	 * @property {number|string} amount
	 * @property {string} currency
	 * @property {string} description
	 * @property {number} matchPrediction
	 */

	/** @type {Receipt[]} */
	let receipts = [];
	let loading = true;
	/** @type {string|null} */
	let error = null;
	/** @type {Receipt|null} */
	let selectedReceipt = null;
	/** @type {ReceiptMatch[]|null} */
	let matches = null;
	/** @type {MatchCandidate[]|null} */
	let candidates = null;
	/** @type {string|null} */
	let matchesError = null;
	/** @type {string|null} */
	let approveError = null;
	/** @type {string|null} */
	let rejectError = null;
	let approving = false;
	let rejecting = false;
	let bankTransactionId = '';
	let matchedAmount = '';
	let matchPredictionOverride = '';
	/** @type {MatchCandidate|null} */
	let selectedCandidate = null;

	onMount(async () => {
		await loadReceipts();
	});

	async function loadReceipts() {
		loading = true;
		error = null;
		try {
			const res = await fetch('/api/v1/receipts');
			if (!res.ok) {
				throw new Error(`Failed to fetch receipts: ${res.status}`);
			}
			receipts = await res.json();
		} catch (err) {
			error = `Error loading receipts: ${getErrorMessage(err)}`;
		} finally {
			loading = false;
		}
	}

	/** @param {unknown} err */
	function getErrorMessage(err) {
		return err instanceof Error ? err.message : String(err);
	}

	/** @param {string} value */
	function parseNumberInput(value) {
		if (!value) {
			return null;
		}
		const normalized = value.replace(/\s+/g, '').replace(',', '.');
		const parsed = Number(normalized);
		return Number.isNaN(parsed) ? null : parsed;
	}

	/** @param {Receipt} receipt */
	async function selectReceipt(receipt) {
		selectedReceipt = receipt;
		matches = null;
		candidates = null;
		matchesError = null;
		approveError = null;
		rejectError = null;
		bankTransactionId = '';
		matchedAmount = receipt?.totalAmount ? String(receipt.totalAmount) : '';
		matchPredictionOverride = '';
		selectedCandidate = null;

		try {
			const [matchesRes, candidatesRes] = await Promise.all([
				fetch(`/api/v1/receipts/${receipt.id}/matches`),
				fetch(`/api/v1/receipts/${receipt.id}/match-candidates`)
			]);

			if (matchesRes.ok) {
				matches = await matchesRes.json();
			} else if (matchesRes.status === 404) {
				matchesError = 'Kvittering ikke funnet';
			} else {
				const errorText = await matchesRes.text();
				matchesError = `Error: ${matchesRes.status} - ${errorText}`;
			}

			if (candidatesRes.ok) {
				candidates = await candidatesRes.json();
			} else if (candidatesRes.status !== 404) {
				const errorText = await candidatesRes.text();
				matchesError = `Error: ${candidatesRes.status} - ${errorText}`;
			}
		} catch (err) {
			matchesError = `Network error: ${getErrorMessage(err)}`;
		}
	}

	function resetSelection() {
		selectedReceipt = null;
		matches = null;
		candidates = null;
		matchesError = null;
		approveError = null;
		rejectError = null;
		bankTransactionId = '';
		matchedAmount = '';
		matchPredictionOverride = '';
		selectedCandidate = null;
	}

	async function approveReceipt() {
		if (!selectedReceipt) {
			return;
		}
		approveError = null;
		rejectError = null;

		const bankId = bankTransactionId.trim();
		if (!bankId) {
			approveError = 'Velg et matchforslag for banktransaksjon';
			return;
		}

		const amountValue = matchedAmount.trim();
		if (!amountValue) {
			approveError = 'Matched amount er paakrevd';
			return;
		}

		const parsedAmount = parseNumberInput(amountValue);
		if (parsedAmount === null || parsedAmount <= 0) {
			approveError = 'Matched amount maa vaere større enn 0';
			return;
		}

		let confidenceValue = null;
		const predictionValue = matchPredictionOverride.trim();
		if (predictionValue) {
			const parsedPrediction = parseNumberInput(predictionValue);
			if (parsedPrediction === null || parsedPrediction < 0 || parsedPrediction > 100) {
				approveError = 'Match prediction maa vaere mellom 0 og 100';
				return;
			}
			confidenceValue = Number((parsedPrediction / 100).toFixed(3));
		}

		approving = true;
		try {
			const res = await fetch('/api/v1/matches', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify({
					receiptId: selectedReceipt.id,
					bankTransactionId: bankId,
					matchedAmount: parsedAmount,
					matchType: 'MANUAL',
					confidence: confidenceValue
				})
			});

			if (!res.ok) {
				const errorText = await res.text();
				approveError = `Godkjenning feilet: ${res.status} - ${errorText}`;
				return;
			}

			matches = await res.json().then((match) => [match]);
		} catch (err) {
			approveError = `Network error: ${getErrorMessage(err)}`;
		} finally {
			approving = false;
		}
	}

	async function rejectReceipt() {
		if (!selectedReceipt) {
			return;
		}
		rejectError = null;
		approveError = null;

		if (!confirm('Er du sikker paa at du vil avvise kvitteringen?')) {
			return;
		}

		rejecting = true;
		try {
			const res = await fetch(`/api/v1/receipts/${selectedReceipt.id}/reject`, {
				method: 'POST'
			});

			if (!res.ok) {
				const errorText = await res.text();
				rejectError = `Avvisning feilet: ${res.status} - ${errorText}`;
				return;
			}

			await loadReceipts();
			resetSelection();
		} catch (err) {
			rejectError = `Network error: ${getErrorMessage(err)}`;
		} finally {
			rejecting = false;
		}
	}

	function isApproved() {
		return Array.isArray(matches) && matches.length > 0;
	}
</script>

<svelte:head>
	<title>Godkjenn kvittering</title>
</svelte:head>

<div class="container">
	<h1>Godkjenn kvitteringer</h1>
	<p>En kvittering maa matches 1-til-1 med en banktransaksjon.</p>

	{#if loading}
		<div class="loading">Laster kvitteringer...</div>
	{:else if error}
		<div class="alert error">{error}</div>
	{:else if !selectedReceipt}
		<div class="receipts-list">
			<table>
				<thead>
					<tr>
						<th>Kvittering-ID</th>
						<th>Dato</th>
						<th>Belop</th>
						<th>Valuta</th>
						<th>Beskrivelse</th>
					</tr>
				</thead>
				<tbody>
					{#each receipts as receipt}
						<tr>
							<td>
								<button class="link-button" on:click={() => selectReceipt(receipt)}>
									{receipt.id}
								</button>
							</td>
							<td>{receipt.purchaseDate || '-'}</td>
							<td>{receipt.totalAmount ?? '-'}</td>
							<td>{receipt.currency || '-'}</td>
							<td>{receipt.description || receipt.merchant || '-'}</td>
						</tr>
					{:else}
						<tr>
							<td colspan="5" class="no-data">Ingen kvitteringer funnet</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{:else}
		<div class="panel">
			<div class="panel-header">
				<h2>Kvittering</h2>
				<div class="panel-actions">
					<button class="btn-secondary" on:click={resetSelection}>Tilbake</button>
					{#if !isApproved()}
						<button class="btn-danger" on:click={rejectReceipt} disabled={rejecting}>
							{rejecting ? 'Avviser...' : 'Avvis kvittering'}
						</button>
					{/if}
				</div>
			</div>
			<div class="receipt-meta">
				<p><strong>ID:</strong> {selectedReceipt.id}</p>
				<p><strong>Dokument-ID:</strong> {selectedReceipt.documentId}</p>
				<p><strong>Dato:</strong> {selectedReceipt.purchaseDate || '-'}</p>
				<p><strong>Belop:</strong> {selectedReceipt.totalAmount ?? '-'} {selectedReceipt.currency || ''}</p>
				<p><strong>Beskrivelse:</strong> {selectedReceipt.description || selectedReceipt.merchant || '-'}</p>
			</div>

			{#if matchesError}
				<div class="alert error">{matchesError}</div>
			{:else if matches === null}
				<div class="loading">Laster match...</div>
			{:else}
				{#if isApproved()}
					<div class="alert success">
						Kvittering er godkjent og matchet til banktransaksjon {matches[0].bankTransactionId}
					</div>
				{/if}
				{#if approveError}
					<div class="alert error">{approveError}</div>
				{/if}
				{#if rejectError}
					<div class="alert error">{rejectError}</div>
				{/if}
				{#if candidates === null}
					<div class="loading">Laster matchforslag...</div>
				{:else if candidates.length === 0}
					<div class="alert info">Ingen matchforslag funnet.</div>
				{:else}
					<div class="candidates">
						<h3>Matchforslag</h3>
						<table>
							<thead>
								<tr>
									<th>Dato</th>
									<th>Belop</th>
									<th>Valuta</th>
									<th>Beskrivelse</th>
									<th>Match</th>
									<th></th>
								</tr>
							</thead>
							<tbody>
								{#each candidates as candidate}
									<tr data-transaction-id={candidate.bankTransactionId}>
										<td>{candidate.bookingDate}</td>
										<td>{candidate.amount}</td>
										<td>{candidate.currency}</td>
										<td>{candidate.description}</td>
										<td>{candidate.matchPrediction}%</td>
										<td>
											<button
												class="btn-secondary"
												on:click={() => {
													const candidateAmount = Number(candidate.amount);
													selectedCandidate = candidate;
													bankTransactionId = candidate.bankTransactionId;
													matchedAmount = Number.isNaN(candidateAmount)
														? ''
														: String(Math.abs(candidateAmount));
													matchPredictionOverride = String(candidate.matchPrediction);
												}}
											>
												Velg
											</button>
										</td>
									</tr>
								{/each}
							</tbody>
						</table>
					</div>
				{/if}
				{#if !isApproved()}
					<div class="approve-form">
						<label for="bankTransactionId">Bank transaction ID</label>
						<input
							id="bankTransactionId"
							type="text"
							bind:value={bankTransactionId}
							placeholder="Velg et matchforslag"
							readonly={selectedCandidate !== null}
						/>
						{#if !selectedCandidate}
							<p class="help-text">Velg et matchforslag over for å fylle banktransaksjon.</p>
						{/if}
						<label for="matchPrediction">Match prediction (%)</label>
						<input
							id="matchPrediction"
							type="number"
							min="0"
							max="100"
							bind:value={matchPredictionOverride}
							placeholder="0-100"
						/>
						<label for="matchedAmount">Matched amount</label>
						<input
							id="matchedAmount"
							type="number"
							step="0.01"
							bind:value={matchedAmount}
						/>
						<button
							class="btn-primary"
							on:click={approveReceipt}
							disabled={approving || !bankTransactionId}
						>
							{approving ? 'Godkjenner...' : 'Godkjenn kvittering'}
						</button>
					</div>
				{/if}
			{/if}
		</div>
	{/if}
</div>

<style>
	.container {
		max-width: 1200px;
	}

	h1 {
		color: #2c3e50;
		margin-bottom: 10px;
	}

	.loading {
		text-align: center;
		padding: 32px;
		color: #666;
	}

	.panel {
		background: white;
		padding: 20px;
		border-radius: 8px;
		box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
		margin-top: 16px;
	}

	.panel-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 16px;
	}

	.panel-actions {
		display: flex;
		align-items: center;
		gap: 10px;
	}

	.receipts-list table {
		width: 100%;
		border-collapse: collapse;
		margin-top: 12px;
	}

	.receipts-list th,
	.receipts-list td {
		text-align: left;
		padding: 10px 12px;
		border-bottom: 1px solid #eee;
		vertical-align: top;
	}

	.link-button {
		background: none;
		border: none;
		color: #2c7be5;
		cursor: pointer;
		padding: 0;
		font: inherit;
		text-align: left;
	}

	.link-button:hover {
		text-decoration: underline;
	}

	.receipt-meta {
		margin-top: 12px;
		color: #444;
	}

	.candidates {
		margin: 16px 0;
	}

	.candidates table {
		width: 100%;
		border-collapse: collapse;
		margin-top: 8px;
	}

	.candidates th,
	.candidates td {
		text-align: left;
		padding: 8px 10px;
		border-bottom: 1px solid #eee;
		vertical-align: top;
	}


	.approve-form {
		margin-top: 16px;
		display: flex;
		flex-direction: column;
		gap: 10px;
		max-width: 420px;
	}

	input {
		padding: 10px;
		border: 1px solid #ddd;
		border-radius: 4px;
		font-size: 14px;
	}

	.btn-primary {
		background: #2c7be5;
		color: white;
		border: none;
		padding: 10px 16px;
		border-radius: 4px;
		font-size: 14px;
		font-weight: 600;
		cursor: pointer;
		transition: background 0.2s;
		align-self: flex-start;
	}

	.btn-primary:disabled {
		background: #9bbcf0;
		cursor: not-allowed;
	}

	.btn-secondary {
		background: #f4f6f8;
		border: 1px solid #ccd2d9;
		color: #2c3e50;
		padding: 8px 12px;
		border-radius: 6px;
		cursor: pointer;
	}

	.btn-danger {
		background: #fceaea;
		border: 1px solid #f3bcbc;
		color: #b73333;
		padding: 8px 12px;
		border-radius: 6px;
		cursor: pointer;
	}

	.btn-danger:disabled {
		opacity: 0.6;
		cursor: not-allowed;
	}

	.alert {
		margin-top: 16px;
		padding: 12px 14px;
		border-radius: 6px;
	}

	.alert.error {
		background: #fee;
		border: 1px solid #fcc;
		color: #c33;
	}

	.alert.success {
		background: #e6f6ec;
		border: 1px solid #cfead7;
		color: #1f7a3e;
	}

	.alert.info {
		background: #eef6ff;
		border: 1px solid #cfe4ff;
		color: #28527a;
	}

	.no-data {
		text-align: center;
		color: #777;
	}

	.help-text {
		margin: 0;
		color: #566573;
		font-size: 13px;
	}
</style>
