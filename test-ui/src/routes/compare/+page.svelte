<script>
	/**
	 * @typedef {Object} DocumentInfo
	 * @property {string} [id]
	 * @property {string} [documentId]
	 */
	/**
	 * @typedef {Object} Job
	 * @property {string} [id]
	 * @property {string} [jobId]
	 * @property {string} [status]
	 */
	/**
	 * @typedef {Object} JobResult
	 * @property {Record<string, unknown>} [fields]
	 * @property {Record<string, number>} [confidence]
	 * @property {Array<Record<string, unknown>>} [transactions]
	 */

	/** @type {FileList | undefined} */
	let files;
	let originalFilename = '';
	let documentType = 'INVOICE';
	let source = 'web';
	let uploading = false;
	/** @type {string|null} */
	let uploadError = null;
	/** @type {DocumentInfo|null} */
	let aiDocument = null;
	/** @type {DocumentInfo|null} */
	let heuristicDocument = null;

	// Job tracking
	/** @type {Job|null} */
	let aiJob = null;
	/** @type {Job|null} */
	let heuristicJob = null;
	/** @type {JobResult|null} */
	let aiResult = null;
	/** @type {JobResult|null} */
	let heuristicResult = null;
	/** @type {string|null} */
	let aiError = null;
	/** @type {string|null} */
	let heuristicError = null;
	let processing = false;
	let currentStep = '';

	$: file = files?.[0];
	$: canUpload = file && originalFilename && !uploading && !processing;
	$: allComplete = aiResult && heuristicResult;

	/** @param {unknown} err */
	function getErrorMessage(err) {
		return err instanceof Error ? err.message : String(err);
	}

	function reset() {
		files = undefined;
		originalFilename = '';
		uploading = false;
		uploadError = null;
		aiDocument = null;
		heuristicDocument = null;
		aiJob = null;
		heuristicJob = null;
		aiResult = null;
		heuristicResult = null;
		aiError = null;
		heuristicError = null;
		processing = false;
		currentStep = '';
	}

	async function handleUploadAndCompare() {
		if (!canUpload) return;

		uploading = true;
		uploadError = null;

		try {
			// Step 1: Upload documents
			currentStep = 'Uploading documents...';
			console.log('Step 1: Uploading documents...');
			const [aiDoc, heuristicDoc] = await Promise.all([
				uploadDocument('AI'),
				uploadDocument('Heuristic')
			]);

			if (!aiDoc || !heuristicDoc) {
				uploadError = uploadError || 'Failed to upload documents';
				uploading = false;
				return;
			}

			aiDocument = aiDoc;
			heuristicDocument = heuristicDoc;
			console.log('Documents uploaded - AI ID:', aiDoc.id, 'Heuristic ID:', heuristicDoc.id);
			uploading = false;
			processing = true;

			// Step 2: Start AI job
			currentStep = 'Starting AI interpretation job...';
			const aiDocId = aiDocument?.documentId || aiDocument?.id;
			console.log('Step 2: Starting AI interpretation job for document:', aiDocId);
			await startAiInterpretation();
			console.log('After startAiInterpretation, aiJob is:', aiJob);
			console.log('aiJob type:', typeof aiJob, 'Has id?', aiJob?.id, 'Has jobId?', aiJob?.jobId);

			const aiJobId = aiJob?.id || aiJob?.jobId;
			if (!aiJob || !aiJobId) {
				console.error('AI job validation failed. aiJob:', aiJob, 'aiJobId:', aiJobId);
				throw new Error(`AI job creation failed - no job ID returned. Job object: ${JSON.stringify(aiJob)}`);
			}
			console.log('AI job validated successfully with ID:', aiJobId);

			// Step 3: Start Heuristic job
			currentStep = 'Starting Heuristic interpretation job...';
			const heuristicDocId = heuristicDocument?.documentId || heuristicDocument?.id;
			console.log('Step 3: Starting Heuristic interpretation job for document:', heuristicDocId);
			await startHeuristicInterpretation();
			console.log('After startHeuristicInterpretation, heuristicJob is:', heuristicJob);
			console.log('heuristicJob type:', typeof heuristicJob, 'Has id?', heuristicJob?.id, 'Has jobId?', heuristicJob?.jobId);

			const heuristicJobId = heuristicJob?.id || heuristicJob?.jobId;
			if (!heuristicJob || !heuristicJobId) {
				console.error('Heuristic job validation failed. heuristicJob:', heuristicJob, 'heuristicJobId:', heuristicJobId);
				throw new Error(`Heuristic job creation failed - no job ID returned. Job object: ${JSON.stringify(heuristicJob)}`);
			}
			console.log('Heuristic job validated successfully with ID:', heuristicJobId);

			// Step 4: Wait for AI job completion
			currentStep = 'Waiting for AI interpretation to complete...';
			console.log('Step 4: Waiting for AI interpretation to complete...');
			await waitForJobCompletion(aiJob, 'AI');
			
			// Step 5: Fetch AI result
			currentStep = 'Fetching AI result...';
			console.log('Step 5: Fetching AI result...');
			await fetchAiResult();

			// Step 6: Wait for Heuristic job completion
			currentStep = 'Waiting for Heuristic interpretation to complete...';
			console.log('Step 6: Waiting for Heuristic interpretation to complete...');
			await waitForJobCompletion(heuristicJob, 'Heuristic');
			
			// Step 7: Fetch Heuristic result
			currentStep = 'Fetching Heuristic result...';
			console.log('Step 7: Fetching Heuristic result...');
			await fetchHeuristicResult();

			console.log('All steps completed!');
			currentStep = '';
			processing = false;
		} catch (err) {
			uploadError = `Error: ${getErrorMessage(err)}`;
			console.error('Error in handleUploadAndCompare:', err);
			uploading = false;
			processing = false;
			currentStep = '';
		}
	}

	/** @param {'AI' | 'Heuristic'} label */
	async function uploadDocument(label) {
		console.log(`Uploading document for ${label}...`);
		if (!file) {
			uploadError = `No file selected for ${label} upload`;
			return null;
		}
		const formData = new FormData();
		formData.append('file', file);
		formData.append('source', source);
		formData.append('originalFilename', `${originalFilename} (${label})`);
		formData.append('documentType', documentType);

		try {
			const res = await fetch('/api/v1/documents', {
				method: 'POST',
				body: formData
			});

			if (!res.ok) {
				const errorData = await res.text();
				uploadError = `Upload failed for ${label}: ${res.status} - ${errorData}`;
				console.error(`Upload failed for ${label}:`, res.status, errorData);
				return null;
			}

			const doc = await res.json();
			console.log(`${label} document uploaded:`, doc);
			return doc;
		} catch (err) {
			uploadError = `Network error during ${label} upload: ${getErrorMessage(err)}`;
			console.error(`Network error during ${label} upload:`, err);
			return null;
		}
	}

	async function startAiInterpretation() {
		console.log('=== Starting AI Interpretation ==>');
		console.log('AI Document:', aiDocument);
		
		const docId = aiDocument?.id || aiDocument?.documentId;
		if (!aiDocument || !docId) {
			aiError = 'AI document not ready';
			console.error('AI document not ready:', aiDocument);
			throw new Error('AI document not ready');
		}

		const request = {
			pipeline: 'AI',
			useOcr: false,
			useAi: true,
			languageHint: 'nb'
		};

		const url = `/api/v1/documents/${docId}/jobs`;
		console.log('Creating AI job - URL:', url);
		console.log('Creating AI job - Request body:', request);

		try {
			const res = await fetch(url, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify(request)
			});

			console.log('AI job creation response status:', res.status);

			if (!res.ok) {
				const errorText = await res.text();
				aiError = `AI job creation failed: ${res.status} - ${errorText}`;
				console.error('AI job creation failed:', aiError);
				throw new Error(aiError);
			}

			const responseText = await res.text();
			console.log('AI job creation raw response:', responseText);
			
			const jobData = JSON.parse(responseText);
			console.log('AI job parsed data:', jobData);
			console.log('Job ID from response:', jobData.id, 'JobId:', jobData.jobId);
			
			aiJob = jobData;
			console.log('aiJob assigned:', aiJob);
		} catch (err) {
			console.error('Exception during AI job creation:', err);
			aiError = `AI job creation error: ${getErrorMessage(err)}`;
			throw err;
		}
	}

	async function startHeuristicInterpretation() {
		console.log('=== Starting Heuristic Interpretation ==>');
		console.log('Heuristic Document:', heuristicDocument);
		
		const docId = heuristicDocument?.id || heuristicDocument?.documentId;
		if (!heuristicDocument || !docId) {
			heuristicError = 'Heuristic document not ready';
			console.error('Heuristic document not ready:', heuristicDocument);
			throw new Error('Heuristic document not ready');
		}

		const request = {
			pipeline: 'HEURISTIC',
			useOcr: false,
			useAi: false,
			languageHint: 'nb'
		};

		const url = `/api/v1/documents/${docId}/jobs`;
		console.log('Creating Heuristic job - URL:', url);
		console.log('Creating Heuristic job - Request body:', request);

		try {
			const res = await fetch(url, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify(request)
			});

			console.log('Heuristic job creation response status:', res.status);

			if (!res.ok) {
				const errorText = await res.text();
				heuristicError = `Heuristic job creation failed: ${res.status} - ${errorText}`;
				console.error('Heuristic job creation failed:', heuristicError);
				throw new Error(heuristicError);
			}

			const responseText = await res.text();
			console.log('Heuristic job creation raw response:', responseText);
			
			const jobData = JSON.parse(responseText);
			console.log('Heuristic job parsed data:', jobData);
			console.log('Job ID from response:', jobData.id, 'JobId:', jobData.jobId);
			
			heuristicJob = jobData;
			console.log('heuristicJob assigned:', heuristicJob);
		} catch (err) {
			console.error('Exception during Heuristic job creation:', err);
			heuristicError = `Heuristic job creation error: ${getErrorMessage(err)}`;
			throw err;
		}
	}

	async function waitForJobCompletion(
		/** @type {Job} */ job,
		/** @type {'AI' | 'Heuristic'} */ jobName
	) {
		console.log(`waitForJobCompletion called for ${jobName} with job:`, job);
		
		const jobId = job?.id || job?.jobId;
		if (!job || !jobId) {
			const error = `${jobName} job not started - job is ${job ? 'missing ID' : 'null'}`;
			console.error(error, 'Job object:', job);
			throw new Error(error);
		}

		const maxAttempts = 60; // 2 minutes max
		let attempts = 0;

		while (attempts < maxAttempts) {
			attempts++;
			console.log(`Checking ${jobName} job status (attempt ${attempts})...`);

			const res = await fetch(`/api/v1/jobs/${jobId}`);
			if (!res.ok) {
				const error = `Failed to check ${jobName} job status: ${res.status}`;
				if (jobName === 'AI') aiError = error;
				else heuristicError = error;
				throw new Error(error);
			}

			const jobStatus = await res.json();
			console.log(`${jobName} job status (attempt ${attempts}):`, jobStatus.status, 'Full status:', jobStatus);

			// Update job object
			if (jobName === 'AI') {
				aiJob = { ...aiJob, ...jobStatus };
			} else {
				heuristicJob = { ...heuristicJob, ...jobStatus };
			}

			if (jobStatus.status === 'COMPLETED') {
				console.log(`${jobName} job completed!`);
				return;
			} else if (jobStatus.status === 'FAILED' || jobStatus.status === 'CANCELLED') {
				const error = `${jobName} job ${jobStatus.status}: ${jobStatus.error || 'Unknown error'}`;
				if (jobName === 'AI') aiError = error;
				else heuristicError = error;
				throw new Error(error);
			}

			// Wait 2 seconds before next check
			await new Promise(resolve => setTimeout(resolve, 2000));
		}

		const error = `${jobName} job timeout`;
		if (jobName === 'AI') aiError = error;
		else heuristicError = error;
		throw new Error(error);
	}

	async function fetchAiResult() {
		const docId = aiDocument?.id || aiDocument?.documentId;
		console.log('Fetching AI result for document:', docId);
		const res = await fetch(`/api/v1/documents/${docId}/result`);
		if (!res.ok) {
			aiError = `Failed to fetch AI result: ${res.status}`;
			console.error('Failed to fetch AI result:', res.status);
			throw new Error(aiError);
		}

		aiResult = await res.json();
		console.log('AI result fetched:', aiResult);
	}

	async function fetchHeuristicResult() {
		const docId = heuristicDocument?.id || heuristicDocument?.documentId;
		console.log('Fetching Heuristic result for document:', docId);
		const res = await fetch(`/api/v1/documents/${docId}/result`);
		if (!res.ok) {
			heuristicError = `Failed to fetch Heuristic result: ${res.status}`;
			console.error('Failed to fetch Heuristic result:', res.status);
			throw new Error(heuristicError);
		}

		heuristicResult = await res.json();
		console.log('Heuristic result fetched:', heuristicResult);
	}

	function getFieldValue(
		/** @type {JobResult | null} */ result,
		/** @type {string} */ fieldName
	) {
		if (!result || !result.fields) return '-';
		const value = result.fields[fieldName];
		return value !== undefined && value !== null ? String(value) : '-';
	}

	function getConfidence(
		/** @type {JobResult | null} */ result,
		/** @type {string} */ fieldName
	) {
		if (!result || !result.confidence) return '-';
		const conf = result.confidence[fieldName];
		return conf !== undefined && conf !== null ? `${(conf * 100).toFixed(1)}%` : '-';
	}

	function getAllFieldNames() {
		const fields = new Set();
		if (aiResult && aiResult.fields) {
			Object.keys(aiResult.fields).forEach((key) => fields.add(key));
		}
		if (heuristicResult && heuristicResult.fields) {
			Object.keys(heuristicResult.fields).forEach((key) => fields.add(key));
		}
		return Array.from(fields).sort();
	}
</script>

<svelte:head>
	<title>Compare AI vs Heuristic - PocketAccount</title>
</svelte:head>

<div class="container">
	<h1>Compare AI vs Heuristic Interpretation</h1>
	<p class="subtitle">
		Upload a PDF document to compare results from AI interpretation and Heuristic interpretation
	</p>

	{#if !aiDocument && !heuristicDocument}
		<!-- Upload Form -->
		<div class="upload-section">
			<h2>Upload Document</h2>
			<div class="upload-form">
				<div class="form-group">
					<label for="file">Select PDF file:</label>
					<input type="file" id="file" accept=".pdf" bind:files disabled={uploading} />
					{#if file}
						<p class="file-info">Selected: {file.name} ({(file.size / 1024).toFixed(2)} KB)</p>
					{/if}
				</div>

				<div class="form-group">
					<label for="originalFilename">Original filename:</label>
					<input
						type="text"
						id="originalFilename"
						bind:value={originalFilename}
						placeholder="e.g. invoice_2026_01.pdf"
						disabled={uploading}
					/>
				</div>

				<div class="form-group">
					<label for="documentType">Document type:</label>
					<select id="documentType" bind:value={documentType} disabled={uploading}>
						<option value="INVOICE">Invoice</option>
						<option value="STATEMENT">Statement</option>
						<option value="RECEIPT">Receipt</option>
						<option value="OTHER">Other</option>
					</select>
				</div>

				<button
					class="btn-primary"
					disabled={!canUpload}
					onclick={handleUploadAndCompare}
				>
					{uploading ? 'Uploading...' : 'Upload & Compare'}
				</button>

				{#if uploadError}
					<div class="error">{uploadError}</div>
				{/if}
			</div>
		</div>
	{:else}
		<!-- Processing and Results Section -->
		<div class="results-section">
			{#if uploadError || aiError || heuristicError}
				<div class="error-banner">
					{#if uploadError}
						<div class="error">Upload Error: {uploadError}</div>
					{/if}
					{#if aiError}
						<div class="error">AI Error: {aiError}</div>
					{/if}
					{#if heuristicError}
						<div class="error">Heuristic Error: {heuristicError}</div>
					{/if}
				</div>
			{/if}

			<div class="document-info">
				<h2>Document: {originalFilename}</h2>
				<p>Type: {documentType}</p>
				{#if aiDocument && heuristicDocument}
				<p class="doc-ids">AI Doc: {aiDocument.documentId || aiDocument.id} | Heuristic Doc: {heuristicDocument.documentId || heuristicDocument.id}</p>
				{/if}
			</div>

			<!-- Job Status -->
			<div class="job-status">
				<div class="job-card">
					<h3>🤖 AI Interpretation</h3>
					{#if aiDocument}
					<p class="doc-id">Document ID: {aiDocument.documentId || aiDocument.id}</p>
					{/if}
					{#if aiJob}
						<p class="job-id">Job ID: {aiJob.id}</p>
						<p class="status status-{aiJob.status?.toLowerCase()}">{aiJob.status || 'PENDING'}</p>
						{#if aiError}
							<div class="error">{aiError}</div>
						{:else if aiResult}
							<div class="success">✓ Completed</div>
						{/if}
					{:else if processing}
						<p class="status">Creating job...</p>
					{:else}
						<p class="status">Waiting...</p>
					{/if}
				</div>

				<div class="job-card">
					<h3>📊 Heuristic Interpretation</h3>
					{#if heuristicDocument}
					<p class="doc-id">Document ID: {heuristicDocument.documentId || heuristicDocument.id}</p>
					{/if}
					{#if heuristicJob}
						<p class="job-id">Job ID: {heuristicJob.id}</p>
						<p class="status status-{heuristicJob.status?.toLowerCase()}">{heuristicJob.status || 'PENDING'}</p>
						{#if heuristicError}
							<div class="error">{heuristicError}</div>
						{:else if heuristicResult}
							<div class="success">✓ Completed</div>
						{/if}
					{:else if processing}
						<p class="status">Creating job...</p>
					{:else}
						<p class="status">Waiting...</p>
					{/if}
				</div>
			</div>

			{#if currentStep && processing}
				<div class="loading">
					<p>{currentStep}</p>
					<div class="spinner"></div>
				</div>
			{/if}

			<!-- Comparison Table -->
			{#if allComplete}
				<div class="comparison-table-container">
					<h2>Comparison Results</h2>
					<table class="comparison-table">
						<thead>
							<tr>
								<th>Field</th>
								<th>AI Result</th>
								<th>AI Confidence</th>
								<th>Heuristic Result</th>
								<th>Heuristic Confidence</th>
								<th>Match</th>
							</tr>
						</thead>
						<tbody>
							{#each getAllFieldNames() as fieldName}
								{@const aiValue = getFieldValue(aiResult, fieldName)}
								{@const heuristicValue = getFieldValue(heuristicResult, fieldName)}
								{@const match = aiValue === heuristicValue}
								<tr class:mismatch={!match && aiValue !== '-' && heuristicValue !== '-'}>
									<td class="field-name">{fieldName}</td>
									<td>{aiValue}</td>
									<td>{getConfidence(aiResult, fieldName)}</td>
									<td>{heuristicValue}</td>
									<td>{getConfidence(heuristicResult, fieldName)}</td>
									<td class="match-indicator">
										{#if aiValue === '-' || heuristicValue === '-'}
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
					{#if (aiResult?.transactions?.length ?? 0) > 0 || (heuristicResult?.transactions?.length ?? 0) > 0}
						<h3>Transactions</h3>
						<div class="transactions-comparison">
							<div class="transaction-column">
								<h4>AI Transactions ({aiResult?.transactions?.length ?? 0})</h4>
								{#if aiResult?.transactions && aiResult.transactions.length > 0}
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
											{#each aiResult.transactions as tx}
												<tr>
													<td>{tx.date || '-'}</td>
													<td>{typeof tx.amount === 'number' ? tx.amount.toFixed(2) : '-'}</td>
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
								<h4>Heuristic Transactions ({heuristicResult?.transactions?.length ?? 0})</h4>
								{#if heuristicResult?.transactions && heuristicResult.transactions.length > 0}
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
											{#each heuristicResult.transactions as tx}
												<tr>
													<td>{tx.date || '-'}</td>
													<td>{typeof tx.amount === 'number' ? tx.amount.toFixed(2) : '-'}</td>
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

					<button class="btn-secondary" onclick={reset}>Compare Another Document</button>
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

	.upload-section {
		max-width: 600px;
		margin: 0 auto;
	}

	.upload-form {
		background: white;
		padding: 2rem;
		border-radius: 8px;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
	}

	.form-group {
		margin-bottom: 1.5rem;
	}

	label {
		display: block;
		margin-bottom: 0.5rem;
		font-weight: 600;
		color: #333;
	}

	input[type='file'],
	input[type='text'],
	select {
		width: 100%;
		padding: 0.75rem;
		border: 1px solid #ddd;
		border-radius: 4px;
		font-size: 1rem;
	}

	.file-info {
		margin-top: 0.5rem;
		font-size: 0.9rem;
		color: #666;
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
		width: 100%;
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
		margin-top: 2rem;
	}

	.btn-secondary:hover {
		background-color: #0b7dda;
	}

	.error {
		margin-top: 1rem;
		padding: 1rem;
		background-color: #ffebee;
		color: #c62828;
		border-radius: 4px;
		border-left: 4px solid #c62828;
	}

	.error-banner {
		margin-bottom: 1.5rem;
	}

	.error-banner .error {
		margin-top: 0;
		margin-bottom: 1rem;
	}

	.error-banner .error:last-child {
		margin-bottom: 0;
	}

	.success {
		margin-top: 1rem;
		padding: 1rem;
		background-color: #e8f5e9;
		color: #2e7d32;
		border-radius: 4px;
		border-left: 4px solid #4caf50;
	}

	.results-section {
		margin-top: 2rem;
	}

	.document-info {
		background: white;
		padding: 1.5rem;
		border-radius: 8px;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
		margin-bottom: 2rem;
	}

	.document-info h2 {
		margin: 0 0 0.5rem 0;
		font-size: 1.5rem;
	}

	.document-info p {
		margin: 0;
		color: #666;
	}

	.doc-ids {
		font-size: 0.85rem;
		margin-top: 0.5rem !important;
	}

	.job-status {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
		gap: 1.5rem;
		margin-bottom: 2rem;
	}

	.job-card {
		background: white;
		padding: 1.5rem;
		border-radius: 8px;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
	}

	.job-card h3 {
		margin: 0 0 1rem 0;
		font-size: 1.25rem;
	}

	.doc-id,
	.job-id {
		font-size: 0.85rem;
		color: #666;
		margin: 0.25rem 0;
	}

	.status {
		font-weight: 600;
		padding: 0.5rem 1rem;
		border-radius: 4px;
		display: inline-block;
	}

	.status-pending,
	.status-running {
		background-color: #fff3cd;
		color: #856404;
	}

	.status-completed {
		background-color: #d4edda;
		color: #155724;
	}

	.status-failed,
	.status-cancelled {
		background-color: #f8d7da;
		color: #721c24;
	}

	.comparison-table-container {
		background: white;
		padding: 2rem;
		border-radius: 8px;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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

	.no-data {
		color: #999;
		font-style: italic;
		padding: 1rem;
	}

	.loading {
		text-align: center;
		padding: 3rem;
		background: white;
		border-radius: 8px;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
		margin-bottom: 2rem;
	}

	.loading p {
		font-size: 1.1rem;
		color: #666;
		margin-bottom: 1.5rem;
	}

	.spinner {
		border: 4px solid #f3f3f3;
		border-top: 4px solid #4caf50;
		border-radius: 50%;
		width: 50px;
		height: 50px;
		animation: spin 1s linear infinite;
		margin: 0 auto;
	}

	@keyframes spin {
		0% {
			transform: rotate(0deg);
		}
		100% {
			transform: rotate(360deg);
		}
	}

	@media (max-width: 768px) {
		.transactions-comparison {
			grid-template-columns: 1fr;
		}

		.comparison-table {
			font-size: 0.85rem;
		}

		.comparison-table th,
		.comparison-table td {
			padding: 0.5rem;
		}
	}
</style>
