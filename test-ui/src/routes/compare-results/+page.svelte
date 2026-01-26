<script>
	import { onMount } from 'svelte';

	let jobs = [];
	let loading = true;
	let error = null;
	let selectedJob1 = null;
	let selectedJob2 = null;
	let result1 = null;
	let result2 = null;
	let comparing = false;

	$: canCompare = selectedJob1 && selectedJob2 && (selectedJob1.jobId || selectedJob1.id) !== (selectedJob2.jobId || selectedJob2.id);

	onMount(async () => {
		await fetchCompletedJobs();
	});

	async function fetchCompletedJobs() {
		loading = true;
		error = null;
		try {
			const res = await fetch('http://localhost:8080/api/v1/interpretation/jobs');
			if (!res.ok) {
				throw new Error(`Failed to fetch jobs: ${res.status}`);
			}
			const allJobs = await res.json();
			console.log('All jobs from API:', allJobs);
			console.log('Total jobs:', allJobs.length);
			// Filter only completed jobs (case-insensitive)
			jobs = allJobs.filter(job => job.status && job.status.toUpperCase() === 'COMPLETED');
			console.log('Completed jobs:', jobs);
			console.log('Completed jobs count:', jobs.length);
		} catch (err) {
			error = `Error loading jobs: ${err.message}`;
			console.error('Error fetching jobs:', err);
		} finally {
			loading = false;
		}
	}

	function selectJob1(job) {
		selectedJob1 = job;
		result1 = null;
		console.log('Selected job 1:', job);
	}

	function selectJob2(job) {
		selectedJob2 = job;
		result2 = null;
		console.log('Selected job 2:', job);
	}

	async function compareResults() {
		if (!canCompare) return;

		comparing = true;
		error = null;
		result1 = null;
		result2 = null;

		try {
			console.log('Fetching results for jobs:', selectedJob1.jobId || selectedJob1.id, selectedJob2.jobId || selectedJob2.id);

			// Fetch both results
			const [res1, res2] = await Promise.all([
				fetch(`http://localhost:8080/api/v1/interpretation/jobs/${selectedJob1.jobId}/result`),
				fetch(`http://localhost:8080/api/v1/interpretation/jobs/${selectedJob2.jobId}/result`)
			]);

			if (!res1.ok) {
				throw new Error(`Failed to fetch result for job 1: ${res1.status}`);
			}
			if (!res2.ok) {
				throw new Error(`Failed to fetch result for job 2: ${res2.status}`);
			}

			result1 = await res1.json();
			result2 = await res2.json();

			console.log('Result 1:', result1);
			console.log('Result 2:', result2);
		} catch (err) {
			error = `Error comparing results: ${err.message}`;
			console.error(err);
		} finally {
			comparing = false;
		}
	}

	function reset() {
		selectedJob1 = null;
		selectedJob2 = null;
		result1 = null;
		result2 = null;
	}

	function getFieldValue(result, fieldName) {
		if (!result || !result.fields) return '-';
		const value = result.fields[fieldName];
		return value !== undefined && value !== null ? String(value) : '-';
	}

	function getConfidence(result, fieldName) {
		if (!result || !result.confidence) return '-';
		const conf = result.confidence[fieldName];
		return conf !== undefined && conf !== null ? `${(conf * 100).toFixed(1)}%` : '-';
	}

	function getAllFieldNames() {
		const fields = new Set();
		if (result1 && result1.fields) {
			Object.keys(result1.fields).forEach((key) => fields.add(key));
		}
		if (result2 && result2.fields) {
			Object.keys(result2.fields).forEach((key) => fields.add(key));
		}
		return Array.from(fields).sort();
	}

	function formatDate(dateString) {
		if (!dateString) return '-';
		const date = new Date(dateString);
		return date.toLocaleString('nb-NO', {
			year: 'numeric',
			month: '2-digit',
			day: '2-digit',
			hour: '2-digit',
			minute: '2-digit'
		});
	}

	function getPipelineLabel(job) {
		// Check extractionMethods field first
		if (job.extractionMethods) {
			if (job.extractionMethods.includes('AI')) {
				return '🤖 AI';
			}
			return '📊 Heuristic';
		}
		// Fallback to useAi field
		if (job.useAi) return '🤖 AI';
		return '📊 Heuristic';
	}
</script>

<svelte:head>
	<title>Compare Job Results - PocketAccount</title>
</svelte:head>

<div class="container">
	<h1>Compare Job Results</h1>
	<p class="subtitle">Select two completed jobs to compare their interpretation results</p>

	{#if error}
		<div class="error">{error}</div>
	{/if}

	{#if loading}
		<div class="loading">
			<p>Loading completed jobs...</p>
			<div class="spinner"></div>
		</div>
	{:else if jobs.length === 0}
		<div class="no-data">
			<p>No completed jobs found.</p>
			<p>Run some interpretation jobs first, then come back to compare results.</p>
			<button class="btn-secondary" onclick={fetchCompletedJobs}>Refresh</button>
		</div>
	{:else}
		<div class="selection-section">
			<div class="job-list">
				<h2>Select First Job</h2>
				<div class="jobs-grid">
					{#each jobs as job}
						<button
							class="job-card"
						class:selected={selectedJob1?.jobId === job.jobId}
							onclick={() => selectJob1(job)}
						>
							<div class="job-header">
								<span class="pipeline-badge">{getPipelineLabel(job)}</span>
								<span class="job-status">✓ {job.status}</span>
						</div>
						<p class="job-filename"><strong>File:</strong> {job.originalFilename || 'Unknown'}</p>
						<p class="job-type"><strong>Type:</strong> {job.documentType || 'Unknown'}</p>							<p class="job-doc">Document: {job.documentId}</p>
						<p class="job-date">{formatDate(job.finishedAt)}</p>
						<p class="job-id">Job ID: {job.jobId}</p>
						</button>
					{/each}
				</div>
			</div>

			<div class="job-list">
				<h2>Select Second Job</h2>
				<div class="jobs-grid">
					{#each jobs as job}
						<button
							class="job-card"
						class:selected={selectedJob2?.jobId === job.jobId}
						class:disabled={selectedJob1?.jobId === job.jobId}
						onclick={() => selectJob2(job)}
						disabled={selectedJob1?.jobId === job.jobId}
						>
							<div class="job-header">
								<span class="pipeline-badge">{getPipelineLabel(job)}</span>
								<span class="job-status">✓ {job.status}</span>
						</div>
						<p class="job-filename"><strong>File:</strong> {job.originalFilename || 'Unknown'}</p>
						<p class="job-type"><strong>Type:</strong> {job.documentType || 'Unknown'}</p>							<p class="job-doc">Document: {job.documentId}</p>
						<p class="job-date">{formatDate(job.finishedAt)}</p>
						<p class="job-id">Job ID: {job.jobId}</p>
						</button>
					{/each}
				</div>
			</div>
		</div>

		<div class="action-section">
			<button class="btn-primary" disabled={!canCompare || comparing} onclick={compareResults}>
				{comparing ? 'Loading...' : 'Compare Selected Jobs'}
			</button>
			<button class="btn-secondary" onclick={reset} disabled={!selectedJob1 && !selectedJob2}>
				Clear Selection
			</button>
		</div>
	{/if}

	{#if result1 && result2}
		<div class="comparison-section">
			<h2>Comparison Results</h2>

			<div class="selected-jobs-info">
				<div class="info-card">
					<h3>Job 1: {getPipelineLabel(selectedJob1)}</h3>
					<p>Document: {selectedJob1.documentId}</p>
					<p>Completed: {formatDate(selectedJob1.finishedAt)}</p>
				</div>
				<div class="info-card">
					<h3>Job 2: {getPipelineLabel(selectedJob2)}</h3>
					<p>Document: {selectedJob2.documentId}</p>
					<p>Completed: {formatDate(selectedJob2.finishedAt)}</p>
				</div>
			</div>

			<table class="comparison-table">
				<thead>
					<tr>
						<th>Field</th>
						<th>Job 1 Result</th>
						<th>Job 1 Confidence</th>
						<th>Job 2 Result</th>
						<th>Job 2 Confidence</th>
						<th>Match</th>
					</tr>
				</thead>
				<tbody>
					{#each getAllFieldNames() as fieldName}
						{@const value1 = getFieldValue(result1, fieldName)}
						{@const value2 = getFieldValue(result2, fieldName)}
						{@const match = value1 === value2}
						<tr class:mismatch={!match && value1 !== '-' && value2 !== '-'}>
							<td class="field-name">{fieldName}</td>
							<td>{value1}</td>
							<td>{getConfidence(result1, fieldName)}</td>
							<td>{value2}</td>
							<td>{getConfidence(result2, fieldName)}</td>
							<td class="match-indicator">
								{#if value1 === '-' || value2 === '-'}
									<span class="badge badge-na">N/A</span>
								{:else if match}
									<span class="badge badge-match">✓ Match</span>
								{:else}
									<span class="badge badge-mismatch">✗ Different</span>
								{/if}
							</td>
						</tr>
					{/each}
				</tbody>
			</table>

			<!-- Transactions Comparison -->
			{#if (result1?.transactions?.length > 0) || (result2?.transactions?.length > 0)}
				<h3>Transactions</h3>
				<div class="transactions-comparison">
					<div class="transaction-column">
						<h4>Job 1 Transactions ({result1?.transactions?.length || 0})</h4>
						{#if result1?.transactions && result1.transactions.length > 0}
							<table class="transaction-table">
								<thead>
									<tr>
										<th>Date</th>
										<th>Amount</th>
										<th>Currency</th>
										<th>Description</th>
									</tr>
								</thead>
								<tbody>
									{#each result1.transactions as tx}
										<tr>
											<td>{tx.date || '-'}</td>
											<td>{tx.amount !== undefined ? tx.amount.toFixed(2) : '-'}</td>
											<td>{tx.currency || '-'}</td>
											<td>{tx.description || '-'}</td>
										</tr>
									{/each}
								</tbody>
							</table>
						{:else}
							<p class="no-data">No transactions found</p>
						{/if}
					</div>

					<div class="transaction-column">
						<h4>Job 2 Transactions ({result2?.transactions?.length || 0})</h4>
						{#if result2?.transactions && result2.transactions.length > 0}
							<table class="transaction-table">
								<thead>
									<tr>
										<th>Date</th>
										<th>Amount</th>
										<th>Currency</th>
										<th>Description</th>
									</tr>
								</thead>
								<tbody>
									{#each result2.transactions as tx}
										<tr>
											<td>{tx.date || '-'}</td>
											<td>{tx.amount !== undefined ? tx.amount.toFixed(2) : '-'}</td>
											<td>{tx.currency || '-'}</td>
											<td>{tx.description || '-'}</td>
										</tr>
									{/each}
								</tbody>
							</table>
						{:else}
							<p class="no-data">No transactions found</p>
						{/if}
					</div>
				</div>
			{/if}
		</div>
	{/if}
</div>

<style>
	.container {
		max-width: 1400px;
		margin: 0 auto;
		padding: 2rem;
	}

	h1 {
		color: #333;
		margin-bottom: 0.5rem;
	}

	.subtitle {
		color: #666;
		margin-bottom: 2rem;
	}

	.loading {
		text-align: center;
		padding: 3rem;
	}

	.spinner {
		border: 4px solid #f3f3f3;
		border-top: 4px solid #4caf50;
		border-radius: 50%;
		width: 50px;
		height: 50px;
		animation: spin 1s linear infinite;
		margin: 1rem auto;
	}

	@keyframes spin {
		0% {
			transform: rotate(0deg);
		}
		100% {
			transform: rotate(360deg);
		}
	}

	.error {
		margin: 1rem 0;
		padding: 1rem;
		background-color: #ffebee;
		color: #c62828;
		border-radius: 4px;
		border-left: 4px solid #c62828;
	}

	.no-data {
		text-align: center;
		padding: 3rem;
		background: white;
		border-radius: 8px;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
	}

	.no-data p {
		color: #666;
		margin-bottom: 1rem;
	}

	.selection-section {
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 2rem;
		margin-bottom: 2rem;
	}

	.job-list h2 {
		font-size: 1.25rem;
		margin-bottom: 1rem;
		color: #333;
	}

	.jobs-grid {
		display: flex;
		flex-direction: column;
		gap: 1rem;
	}

	.job-card {
		background: white;
		border: 2px solid #ddd;
		border-radius: 8px;
		padding: 1rem;
		text-align: left;
		cursor: pointer;
		transition: all 0.2s;
	}

	.job-card:hover:not(:disabled) {
		border-color: #4caf50;
		box-shadow: 0 2px 8px rgba(76, 175, 80, 0.2);
	}

	.job-card.selected {
		border-color: #4caf50;
		background-color: #e8f5e9;
	}

	.job-card.disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}

	.job-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 0.5rem;
	}

	.pipeline-badge {
		font-weight: 600;
		font-size: 1rem;
	}

	.job-status {
		color: #4caf50;
		font-size: 0.9rem;
		font-weight: 600;
	}

	.job-doc {
		font-size: 0.9rem;
		color: #333;
		margin: 0.25rem 0;
		word-break: break-all;
	}

	.job-date {
		font-size: 0.85rem;
		color: #666;
		margin: 0.25rem 0;
	}

	.job-id {
		font-size: 0.75rem;
		color: #999;
		margin: 0.25rem 0;
		font-family: monospace;
	}

	.action-section {
		display: flex;
		gap: 1rem;
		justify-content: center;
		margin: 2rem 0;
	}

	.btn-primary,
	.btn-secondary {
		padding: 0.75rem 1.5rem;
		border: none;
		border-radius: 4px;
		font-size: 1rem;
		font-weight: 600;
		cursor: pointer;
		transition: background-color 0.2s;
	}

	.btn-primary {
		background-color: #4caf50;
		color: white;
	}

	.btn-primary:hover:not(:disabled) {
		background-color: #45a049;
	}

	.btn-primary:disabled {
		background-color: #ccc;
		cursor: not-allowed;
	}

	.btn-secondary {
		background-color: #2196f3;
		color: white;
	}

	.btn-secondary:hover:not(:disabled) {
		background-color: #0b7dda;
	}

	.btn-secondary:disabled {
		background-color: #ccc;
		cursor: not-allowed;
	}

	.comparison-section {
		background: white;
		padding: 2rem;
		border-radius: 8px;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
		margin-top: 2rem;
	}

	.selected-jobs-info {
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 1.5rem;
		margin-bottom: 2rem;
	}

	.info-card {
		background: #f5f5f5;
		padding: 1rem;
		border-radius: 4px;
	}

	.info-card h3 {
		margin: 0 0 0.5rem 0;
		font-size: 1.1rem;
	}

	.info-card p {
		margin: 0.25rem 0;
		font-size: 0.9rem;
		color: #666;
	}

	.comparison-table {
		width: 100%;
		border-collapse: collapse;
		margin-top: 1rem;
	}

	.comparison-table th,
	.comparison-table td {
		padding: 0.75rem;
		text-align: left;
		border-bottom: 1px solid #ddd;
	}

	.comparison-table th {
		background-color: #f5f5f5;
		font-weight: 600;
		color: #333;
	}

	.comparison-table tr.mismatch {
		background-color: #fff3cd;
	}

	.field-name {
		font-weight: 600;
		color: #333;
	}

	.match-indicator {
		text-align: center;
	}

	.badge {
		padding: 0.25rem 0.75rem;
		border-radius: 12px;
		font-size: 0.85rem;
		font-weight: 600;
		display: inline-block;
	}

	.badge-match {
		background-color: #d4edda;
		color: #155724;
	}

	.badge-mismatch {
		background-color: #f8d7da;
		color: #721c24;
	}

	.badge-na {
		background-color: #e0e0e0;
		color: #666;
	}

	.transactions-comparison {
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 2rem;
		margin-top: 2rem;
	}

	.transaction-column h4 {
		margin-bottom: 1rem;
		color: #333;
	}

	.transaction-table {
		width: 100%;
		border-collapse: collapse;
		font-size: 0.9rem;
	}

	.transaction-table th,
	.transaction-table td {
		padding: 0.5rem;
		text-align: left;
		border-bottom: 1px solid #eee;
	}

	.transaction-table th {
		background-color: #f9f9f9;
		font-weight: 600;
	}

	@media (max-width: 768px) {
		.selection-section {
			grid-template-columns: 1fr;
		}

		.selected-jobs-info {
			grid-template-columns: 1fr;
		}

		.transactions-comparison {
			grid-template-columns: 1fr;
		}
	}
</style>
