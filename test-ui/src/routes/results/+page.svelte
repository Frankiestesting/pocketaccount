<script>
	import { onMount } from 'svelte';

	/**
	 * @typedef {Object} Job
	 * @property {string} jobId
	 * @property {string} documentId
	 * @property {string} status
	 * @property {string} [documentType]
	 * @property {string} [originalFilename]
	 * @property {string} [created]
	 * @property {string} [startedAt]
	 * @property {string} [finishedAt]
	 * @property {string} [error]
	 */
	/**
	 * @typedef {Object} JobResult
	 * @property {string} [documentType]
	 * @property {string} [interpretedAt]
	 * @property {Object} [fields]
	 * @property {Object} [confidence]
	 * @property {InvoiceFields} [invoiceFields]
	 * @property {InvoiceFields} [invoiceData]
	 * @property {Array<{amount?: number, date?: string, currency?: string, description?: string}>} [transactions]
	 */
	/**
	 * @typedef {Object} InvoiceFields
	 * @property {number} [amount]
	 * @property {string} [currency]
	 * @property {string} [date]
	 * @property {string} [sender]
	 * @property {string} [description]
	 */

	/** @type {Job[]} */
	let jobs = [];
	let loading = true;
	/** @type {string|null} */
	let error = null;
	/** @type {string|null} */
	let deleteError = null;
	/** @type {Set<string>} */
	let deletingIds = new Set();
	/** @type {Job|null} */
	let selectedJob = null;
	/** @type {JobResult|null} */
	let jobResult = null;
	/** @type {string|null} */
	let resultError = null;
	/** @type {string|null} */
	let receiptCreateError = null;
	/** @type {{ id?: string } | null} */
	let receiptCreated = null;

	onMount(() => {
		loadJobs();
		// Auto-refresh every 5 seconds
		const interval = setInterval(loadJobs, 5000);
		return () => clearInterval(interval);
	});

	/** @param {unknown} err */
	function getErrorMessage(err) {
		return err instanceof Error ? err.message : String(err);
	}

	async function loadJobs() {
		const wasLoading = loading;
		if (!wasLoading) loading = false; // Don't show loading on refresh
		error = null;

		try {
			const res = await fetch('/api/v1/interpretation/jobs');
			if (res.ok) {
				const data = /** @type {Job[]} */ (await res.json());
				jobs = data.sort(
					(a, b) => new Date(b.startedAt || 0).getTime() - new Date(a.startedAt || 0).getTime()
				);
			} else {
				error = `Error loading jobs: ${res.status}`;
			}
		} catch (err) {
			error = `Network error: ${getErrorMessage(err)}`;
		} finally {
			loading = false;
		}
	}

	/** @param {Job} job */
	async function viewResult(job) {
		if (job.status !== 'COMPLETED') {
			return;
		}

		selectedJob = job;
		resultError = null;
		jobResult = null;
		receiptCreateError = null;
		receiptCreated = null;

		try {
			const res = await fetch(`/api/v1/interpretation/jobs/${job.jobId}/result`);
			if (res.ok) {
				jobResult = await res.json();
			} else if (res.status === 404) {
				resultError = 'Resultat ikke funnet';
			} else {
				const errorText = await res.text();
				resultError = `Error: ${res.status} - ${errorText}`;
			}
		} catch (err) {
			resultError = `Network error: ${getErrorMessage(err)}`;
		}
	}

	/** @param {string} jobId */
	async function hasApprovedTransactions(jobId) {
		try {
			const res = await fetch(`/api/v1/interpretation/jobs/${jobId}/statement-transactions`);
			if (!res.ok) {
				if (res.status === 404) {
					return false;
				}
				const errorText = await res.text();
				throw new Error(`Error: ${res.status} - ${errorText}`);
			}
			const transactions = await res.json();
			return Array.isArray(transactions) && transactions.some((tx) => tx?.approved);
		} catch (err) {
			deleteError = `Kunne ikke sjekke transaksjoner: ${getErrorMessage(err)}`;
			return true;
		}
	}

	/** @param {Job} job */
	async function deleteResult(job) {
		if (!job?.jobId || deletingIds.has(job.jobId)) {
			return;
		}

		const confirmed = window.confirm('Slett jobb og resultat? Underliggende dokument beholdes. Dette kan ikke angres.');
		if (!confirmed) {
			return;
		}

		deleteError = null;
		deletingIds = new Set(deletingIds).add(job.jobId);
		try {
			const hasApproved = await hasApprovedTransactions(job.jobId);
			if (hasApproved) {
				deleteError = 'Kan ikke slette: minst en transaksjon er godkjent.';
				return;
			}

			const res = await fetch(`/api/v1/interpretation/jobs/${job.jobId}`, {
				method: 'DELETE'
			});
			if (!res.ok) {
				const errorText = await res.text();
				deleteError = `Sletting feilet: ${res.status} - ${errorText}`;
				return;
			}

			jobs = jobs.filter((item) => item.jobId !== job.jobId);
			if (selectedJob?.jobId === job.jobId) {
				closeResult();
			}
		} catch (err) {
			deleteError = `Nettverksfeil: ${getErrorMessage(err)}`;
		} finally {
			const next = new Set(deletingIds);
			next.delete(job.jobId);
			deletingIds = next;
		}
	}

	function closeResult() {
		selectedJob = null;
		jobResult = null;
		resultError = null;
		receiptCreateError = null;
		receiptCreated = null;
	}

	/** @param {JobResult|null} result */
	function getInvoiceFields(result) {
		return result?.invoiceFields || result?.invoiceData || {};
	}

	async function createReceiptFromResult() {
		if (!selectedJob) {
			return;
		}
		receiptCreateError = null;
		receiptCreated = null;
		try {
			const res = await fetch(`/api/v1/interpretation/documents/${selectedJob.documentId}/receipt`, {
				method: 'POST'
			});
			if (!res.ok) {
				const errorText = await res.text();
				receiptCreateError = `Oppretting feilet: ${res.status} - ${errorText}`;
				return;
			}
			receiptCreated = await res.json();
		} catch (err) {
			receiptCreateError = `Network error: ${getErrorMessage(err)}`;
		}
	}

	/** @param {string} status */
	function getStatusClass(status) {
		switch (status) {
			case 'COMPLETED':
				return 'status-completed';
			case 'FAILED':
				return 'status-failed';
			case 'RUNNING':
				return 'status-running';
			case 'PENDING':
				return 'status-pending';
			case 'CANCELLED':
				return 'status-cancelled';
			default:
				return '';
		}
	}
</script>

<svelte:head>
	<title>Resultater - PocketAccount</title>
</svelte:head>

<div class="container">
	<h1>Tolkingsresultater</h1>

	{#if loading}
		<div class="loading">Laster jobber...</div>
	{:else if error}
		<div class="alert alert-error">{error}</div>
	{:else if !selectedJob}
		<div class="jobs-list">
			<p class="info">Klikk på en fullført jobb for å se resultatet. Listen oppdateres automatisk.</p>
			{#if deleteError}
				<div class="alert alert-error">{deleteError}</div>
			{/if}
			<table>
				<thead>
					<tr>
						<th>ID</th>
						<th>Dokument-ID</th>
						<th>Type</th>
						<th>Filnavn</th>
						<th>Startet</th>
						<th>Status</th>
						<th>Feil</th>
						<th>Fullført</th>
						<th></th>
					</tr>
				</thead>
				<tbody>
					{#each jobs as job}
						<tr>
							<td>
								{#if job.status === 'COMPLETED'}
									<button class="job-id-btn" on:click={() => viewResult(job)}>
										{job.jobId}
									</button>
								{:else}
									<span class="job-id-text">{job.jobId}</span>
								{/if}
							</td>
							<td>{job.documentId}</td>
							<td>{job.documentType || '-'}</td>
							<td>{job.originalFilename || '-'}</td>
							<td>{job.created ? new Date(job.created).toLocaleString('nb-NO') : '-'}</td>
							<td>
								<span class="status-badge {getStatusClass(job.status)}">
									{job.status}
								</span>
							</td>
							<td>
								{#if job.status === 'FAILED'}
									<span class="error-text">{job.error || 'Ukjent feil'}</span>
								{:else}
									-
								{/if}
							</td>
							<td>{job.finishedAt ? new Date(job.finishedAt).toLocaleString('nb-NO') : '-'}</td>
							<td class="action-cell">
								<button
									class="icon-button"
									on:click={() => deleteResult(job)}
									disabled={job.status === 'RUNNING' || deletingIds.has(job.jobId)}
									title="Slett resultat"
								>
									<svg viewBox="0 0 24 24" aria-hidden="true">
										<path d="M9 3h6l1 2h4v2H4V5h4l1-2zm1 6h2v9h-2V9zm4 0h2v9h-2V9z" />
									</svg>
								</button>
							</td>
						</tr>
					{:else}
						<tr>
							<td colspan="9" class="no-data">Ingen jobber funnet</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{:else}
		<div class="result-panel">
			<div class="panel-header">
				<h2>Resultat for jobb #{selectedJob.jobId}</h2>
				<button on:click={closeResult} class="btn-secondary">Tilbake</button>
			</div>

			<div class="job-info">
				<p><strong>Dokument-ID:</strong> {selectedJob.documentId}</p>
				<p><strong>Type:</strong> {selectedJob.documentType}</p>
				<p><strong>Filnavn:</strong> {selectedJob.originalFilename}</p>
				<p><strong>Startet:</strong> {selectedJob.startedAt ? new Date(selectedJob.startedAt).toLocaleString('nb-NO') : '-'}</p>
				<p><strong>Fullført:</strong> {selectedJob.finishedAt ? new Date(selectedJob.finishedAt).toLocaleString('nb-NO') : '-'}</p>
			</div>

			{#if resultError}
				<div class="alert alert-error">
					<strong>Feil:</strong> {resultError}
				</div>
			{:else if deleteError}
				<div class="alert alert-error">
					<strong>Feil:</strong> {deleteError}
				</div>
			{:else if jobResult}
				<div class="result-content">
					<h3>Ekstraherte data</h3>

					{#if jobResult.documentType === 'RECEIPT' || selectedJob.documentType === 'RECEIPT'}
						<div class="receipt-actions">
							<button class="btn-primary" on:click={createReceiptFromResult}>
								Opprett kvittering
							</button>
							{#if receiptCreateError}
								<div class="alert alert-error">{receiptCreateError}</div>
							{/if}
							{#if receiptCreated}
								<div class="alert alert-success">Kvittering opprettet ({receiptCreated.id})</div>
							{/if}
						</div>
					{/if}

					{#if jobResult.invoiceFields || jobResult.invoiceData}
						{@const invoice = getInvoiceFields(jobResult)}
						<div class="data-section">
							<h4>Fakturainformasjon</h4>
							<div class="data-grid">
								<div class="data-item">
									<span class="label">Beløp:</span>
									<span class="value">
										{invoice.amount ?? '-'} {invoice.currency || ''}
									</span>
								</div>
								<div class="data-item">
									<span class="label">Dato:</span>
									<span class="value">{invoice.date || '-'}</span>
								</div>
								<div class="data-item">
									<span class="label">Avsender:</span>
									<span class="value">{invoice.sender || '-'}</span>
								</div>
								<div class="data-item">
									<span class="label">Beskrivelse:</span>
									<span class="value">{invoice.description || '-'}</span>
								</div>
							</div>
						</div>
					{/if}

					{#if jobResult.transactions && jobResult.transactions.length > 0}
						<div class="data-section">
							<h4>Transaksjoner ({jobResult.transactions.length})</h4>
							<table class="transactions-table">
								<thead>
									<tr>
										<th>Dato</th>
										<th>Beløp</th>
										<th>Valuta</th>
										<th>Beskrivelse</th>
									</tr>
								</thead>
								<tbody>
									{#each jobResult.transactions as tx}
										<tr>
											<td>{tx.date || '-'}</td>
											<td class:negative={(typeof tx.amount === 'number' ? tx.amount : 0) < 0}>
												{typeof tx.amount === 'number' ? tx.amount : '-'}
											</td>
											<td>{tx.currency}</td>
											<td>{tx.description || '-'}</td>
										</tr>
									{/each}
								</tbody>
							</table>
						</div>
					{/if}

					<div class="metadata-section">
						<h4>Metadata</h4>
						<p><strong>Tolket:</strong> {jobResult.interpretedAt ? new Date(jobResult.interpretedAt).toLocaleString('nb-NO') : '-'}</p>
					</div>

					<details class="raw-data">
						<summary>Vis rådata (JSON)</summary>
						<pre>{JSON.stringify(jobResult, null, 2)}</pre>
					</details>
				</div>
			{:else}
				<div class="loading">Laster resultat...</div>
			{/if}
		</div>
	{/if}
</div>

<style>
	.container {
		max-width: 1400px;
	}

	h1 {
		color: #2c3e50;
		margin-bottom: 30px;
	}

	.loading {
		text-align: center;
		padding: 40px;
		color: #666;
	}

	.alert {
		padding: 20px;
		border-radius: 8px;
		margin: 20px 0;
	}

	.alert-success {
		background: #e6f6ec;
		border: 1px solid #cfead7;
		color: #1f7a3e;
	}

	.receipt-actions {
		margin: 16px 0;
		display: flex;
		flex-direction: column;
		gap: 10px;
		max-width: 360px;
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

	.action-cell {
		width: 56px;
	}

	.icon-button {
		background: #e74c3c;
		border: none;
		border-radius: 6px;
		width: 34px;
		height: 34px;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		transition: background 0.2s;
	}

	.icon-button:disabled {
		background: #f0b1aa;
		cursor: not-allowed;
	}

	.icon-button svg {
		width: 18px;
		height: 18px;
		fill: white;
	}

	.alert-error {
		background: #fee;
		border: 1px solid #fcc;
		color: #c33;
	}

	.info {
		background: #e3f2fd;
		padding: 12px;
		border-radius: 4px;
		margin-bottom: 20px;
		color: #1976d2;
	}

	.jobs-list {
		background: white;
		padding: 20px;
		border-radius: 8px;
		box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
	}

	table {
		width: 100%;
		border-collapse: collapse;
	}

	th {
		background: #f8f9fa;
		padding: 12px;
		text-align: left;
		font-weight: 600;
		color: #2c3e50;
		border-bottom: 2px solid #dee2e6;
		font-size: 14px;
	}

	td {
		padding: 12px;
		border-bottom: 1px solid #dee2e6;
		font-size: 14px;
	}

	tr:hover {
		background: #f8f9fa;
	}

	.job-id-btn {
		background: #3498db;
		color: white;
		border: none;
		padding: 6px 12px;
		border-radius: 4px;
		cursor: pointer;
		font-weight: 600;
		transition: background 0.2s;
	}

	.job-id-btn:hover {
		background: #2980b9;
	}

	.job-id-text {
		color: #666;
	}

	.status-badge {
		padding: 4px 8px;
		border-radius: 4px;
		font-size: 12px;
		font-weight: 600;
		text-transform: uppercase;
	}

	.status-completed {
		background: #d4edda;
		color: #155724;
	}

	.status-failed {
		background: #f8d7da;
		color: #721c24;
	}

	.status-running {
		background: #fff3cd;
		color: #856404;
	}

	.status-pending {
		background: #d1ecf1;
		color: #0c5460;
	}

	.status-cancelled {
		background: #e2e3e5;
		color: #383d41;
	}

	.error-text {
		color: #c0392b;
		font-weight: 600;
		display: inline-block;
		max-width: 420px;
		white-space: normal;
		word-break: break-word;
	}

	.no-data {
		text-align: center;
		color: #999;
		font-style: italic;
	}

	.result-panel {
		background: white;
		padding: 30px;
		border-radius: 8px;
		box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
	}

	.panel-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 20px;
		padding-bottom: 20px;
		border-bottom: 1px solid #dee2e6;
	}

	.panel-header h2 {
		margin: 0;
		color: #2c3e50;
	}

	.job-info {
		background: #f8f9fa;
		padding: 15px;
		border-radius: 4px;
		margin-bottom: 20px;
	}

	.job-info p {
		margin: 5px 0;
	}

	.result-content {
		margin-top: 20px;
	}

	.result-content h3 {
		color: #2c3e50;
		margin-bottom: 20px;
	}

	.data-section {
		background: #f8f9fa;
		padding: 20px;
		border-radius: 4px;
		margin-bottom: 20px;
	}

	.data-section h4 {
		margin: 0 0 15px 0;
		color: #2c3e50;
	}

	.data-grid {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
		gap: 15px;
	}

	.data-item {
		display: flex;
		flex-direction: column;
	}

	.data-item .label {
		font-size: 12px;
		color: #666;
		margin-bottom: 4px;
	}

	.data-item .value {
		font-size: 16px;
		font-weight: 600;
		color: #2c3e50;
	}

	.transactions-table {
		margin-top: 10px;
	}

	.transactions-table th,
	.transactions-table td {
		padding: 10px;
	}

	.negative {
		color: #c33;
	}

	.metadata-section {
		background: #e3f2fd;
		padding: 15px;
		border-radius: 4px;
		margin-bottom: 20px;
	}

	.metadata-section h4 {
		margin: 0 0 10px 0;
		color: #1976d2;
	}

	.metadata-section p {
		margin: 5px 0;
		color: #1976d2;
	}

	.raw-data {
		margin-top: 20px;
	}

	.raw-data summary {
		cursor: pointer;
		padding: 10px;
		background: #f8f9fa;
		border-radius: 4px;
		font-weight: 600;
		color: #2c3e50;
	}

	.raw-data pre {
		margin-top: 10px;
		padding: 15px;
		background: #f8f9fa;
		border-radius: 4px;
		overflow-x: auto;
		font-size: 12px;
	}

	.btn-secondary {
		background: #95a5a6;
		color: white;
		border: none;
		padding: 8px 16px;
		border-radius: 4px;
		font-size: 14px;
		cursor: pointer;
		transition: background 0.2s;
	}

	.btn-secondary:hover {
		background: #7f8c8d;
	}
</style>
