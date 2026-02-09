<script>
	import { onMount } from 'svelte';

	let jobs = [];
	let loading = true;
	let error = null;
	let selectedJob = null;
	let jobResult = null;
	let resultError = null;

	onMount(async () => {
		await loadJobs();
		// Auto-refresh every 5 seconds
		const interval = setInterval(loadJobs, 5000);
		return () => clearInterval(interval);
	});

	async function loadJobs() {
		const wasLoading = loading;
		if (!wasLoading) loading = false; // Don't show loading on refresh
		error = null;

		try {
			const res = await fetch('/api/v1/interpretation/jobs');
			if (res.ok) {
				const data = await res.json();
				jobs = data;
			} else {
				error = `Error loading jobs: ${res.status}`;
			}
		} catch (err) {
			error = `Network error: ${err.message}`;
		} finally {
			loading = false;
		}
	}

	async function viewResult(job) {
		if (job.status !== 'COMPLETED') {
			return;
		}

		selectedJob = job;
		resultError = null;
		jobResult = null;

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
			resultError = `Network error: ${err.message}`;
		}
	}

	function closeResult() {
		selectedJob = null;
		jobResult = null;
		resultError = null;
	}

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
						</tr>
					{:else}
						<tr>
							<td colspan="8" class="no-data">Ingen jobber funnet</td>
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
			{:else if jobResult}
				<div class="result-content">
					<h3>Ekstraherte data</h3>

					{#if jobResult.invoiceData}
						<div class="data-section">
							<h4>Fakturainformasjon</h4>
							<div class="data-grid">
								<div class="data-item">
									<span class="label">Beløp:</span>
									<span class="value">{jobResult.invoiceData.amount} {jobResult.invoiceData.currency}</span>
								</div>
								<div class="data-item">
									<span class="label">Dato:</span>
									<span class="value">{jobResult.invoiceData.date || '-'}</span>
								</div>
								<div class="data-item">
									<span class="label">Avsender:</span>
									<span class="value">{jobResult.invoiceData.sender || '-'}</span>
								</div>
								<div class="data-item">
									<span class="label">Beskrivelse:</span>
									<span class="value">{jobResult.invoiceData.description || '-'}</span>
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
											<td class:negative={tx.amount < 0}>{tx.amount}</td>
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
