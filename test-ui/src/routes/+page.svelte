<script>
	let files = [];
	let source = 'mobile';
	let originalFilename = '';
	let documentType = 'INVOICE';
	let response = null;
	let error = null;
	let documentId = '';
	let documentInfo = null;
	let getError = null;
	let jobResponse = null;
	let jobError = null;
	let pipeline = 'DEFAULT';
	let useOcr = true;
	let useAi = true;
	let languageHint = 'nb';
	let jobId = '';
	let jobStatus = null;
	let jobStatusError = null;
	let cancelJobResponse = null;
	let cancelJobError = null;
	let extractionResult = null;
	let extractionResultError = null;
	let correctionResponse = null;
	let correctionError = null;
	let correctionDocumentType = 'INVOICE';
	let correctionFields = '{"date": "2026-01-02", "amount": 12450.00, "currency": "NOK", "sender": "Strøm AS", "description": "Strøm - januar 2026"}';
	let correctionNote = 'Rettet beskrivelse';

	$: file = files[0];

	async function handleSubmit(event) {
		event.preventDefault();
		if (!file) {
			error = 'Please select a file';
			return;
		}
		if (!originalFilename) {
			error = 'Please enter original filename';
			return;
		}

		const formData = new FormData();
		formData.append('file', file);
		formData.append('source', source);
		formData.append('originalFilename', originalFilename);
		formData.append('documentType', documentType);

		try {
			const res = await fetch('http://localhost:8080/api/v1/documents', {
				method: 'POST',
				body: formData
			});
			if (res.ok) {
				response = await res.json();
				error = null;
			} else {
				error = `Error: ${res.status} ${res.statusText}`;
				response = null;
			}
		} catch (err) {
			error = `Network error: ${err.message}`;
			response = null;
		}
	}

	async function getDocumentInfo() {
		if (!documentId.trim()) {
			getError = 'Please enter a document ID';
			return;
		}

		try {
			const res = await fetch(`http://localhost:8080/api/v1/documents/${documentId}`);
			if (res.ok) {
				documentInfo = await res.json();
				getError = null;
			} else if (res.status === 404) {
				getError = 'Document not found';
				documentInfo = null;
			} else {
				getError = `Error: ${res.status} ${res.statusText}`;
				documentInfo = null;
			}
		} catch (err) {
			getError = `Network error: ${err.message}`;
			documentInfo = null;
		}
	}

	async function previewDocument() {
		if (!documentId.trim()) {
			getError = 'Please enter a document ID';
			return;
		}

		try {
			const res = await fetch(`http://localhost:8080/api/v1/documents/${documentId}/file`);
			if (res.ok) {
				// Create a blob from the response and open it in a new tab
				const blob = await res.blob();
				const url = URL.createObjectURL(blob);
				window.open(url, '_blank');
				getError = null;
			} else if (res.status === 404) {
				getError = 'Document file not found';
			} else {
				getError = `Error: ${res.status} ${res.statusText}`;
			}
		} catch (err) {
			getError = `Network error: ${err.message}`;
		}
	}

	async function createJob() {
		if (!documentId.trim()) {
			jobError = 'Please enter a document ID';
			return;
		}

		const jobRequest = {
			pipeline: pipeline,
			useOcr: useOcr,
			useAi: useAi,
			languageHint: languageHint
		};

		try {
			const res = await fetch(`http://localhost:8080/api/v1/documents/${documentId}/jobs`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify(jobRequest)
			});
			if (res.status === 202) {
				jobResponse = await res.json();
				jobError = null;
			} else if (res.status === 404) {
				jobError = 'Document not found';
				jobResponse = null;
			} else {
				jobError = `Error: ${res.status} ${res.statusText}`;
				jobResponse = null;
			}
		} catch (err) {
			jobError = `Network error: ${err.message}`;
			jobResponse = null;
		}
	}

	async function getJobStatus() {
		if (!jobId.trim()) {
			jobStatusError = 'Please enter a job ID';
			return;
		}

		try {
			const res = await fetch(`http://localhost:8080/api/v1/jobs/${jobId}`);
			if (res.ok) {
				jobStatus = await res.json();
				jobStatusError = null;
			} else if (res.status === 404) {
				jobStatusError = 'Job not found';
				jobStatus = null;
			} else {
				jobStatusError = `Error: ${res.status} ${res.statusText}`;
				jobStatus = null;
			}
		} catch (err) {
			jobStatusError = `Network error: ${err.message}`;
			jobStatus = null;
		}
	}

	async function cancelJob() {
		if (!jobId.trim()) {
			cancelJobError = 'Please enter a job ID';
			return;
		}

		try {
			const res = await fetch(`http://localhost:8080/api/v1/jobs/${jobId}/cancel`, {
				method: 'POST'
			});
			if (res.ok) {
				cancelJobResponse = await res.json();
				cancelJobError = null;
			} else if (res.status === 404) {
				cancelJobError = 'Job not found';
				cancelJobResponse = null;
			} else if (res.status === 400) {
				cancelJobError = 'Job cannot be cancelled (already completed or failed)';
				cancelJobResponse = null;
			} else {
				cancelJobError = `Error: ${res.status} ${res.statusText}`;
				cancelJobResponse = null;
			}
		} catch (err) {
			cancelJobError = `Network error: ${err.message}`;
			cancelJobResponse = null;
		}
	}

	async function getExtractionResult() {
		if (!documentId.trim()) {
			extractionResultError = 'Please enter a document ID';
			return;
		}

		try {
			const res = await fetch(`http://localhost:8080/api/v1/documents/${documentId}/result`);
			if (res.ok) {
				extractionResult = await res.json();
				extractionResultError = null;
			} else if (res.status === 404) {
				extractionResultError = 'Document not found or no extraction result available';
				extractionResult = null;
			} else {
				extractionResultError = `Error: ${res.status} ${res.statusText}`;
				extractionResult = null;
			}
		} catch (err) {
			extractionResultError = `Network error: ${err.message}`;
			extractionResult = null;
		}
	}

	async function saveCorrection() {
		if (!documentId.trim()) {
			correctionError = 'Please enter a document ID';
			return;
		}

		let fields;
		try {
			fields = JSON.parse(correctionFields);
		} catch (e) {
			correctionError = 'Invalid JSON in fields';
			return;
		}

		const correctionRequest = {
			documentType: correctionDocumentType,
			fields: fields,
			note: correctionNote
		};

		try {
			const res = await fetch(`http://localhost:8080/api/v1/documents/${documentId}/correction`, {
				method: 'PUT',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify(correctionRequest)
			});
			if (res.ok) {
				correctionResponse = await res.json();
				correctionError = null;
			} else if (res.status === 404) {
				correctionError = 'Document not found';
				correctionResponse = null;
			} else {
				correctionError = `Error: ${res.status} ${res.statusText}`;
				correctionResponse = null;
			}
		} catch (err) {
			correctionError = `Network error: ${err.message}`;
			correctionResponse = null;
		}
	}
</script>

<svelte:head>
	<title>Document Upload Test</title>
</svelte:head>

<section>
	<h1>Upload PDF Document</h1>
	<form on:submit={handleSubmit}>
		<div>
			<label for="file">Select PDF File:</label>
			<input type="file" id="file" accept=".pdf" bind:files />
		</div>
		<div>
			<label for="source">Source:</label>
			<select id="source" bind:value={source}>
				<option value="mobile">Mobile</option>
				<option value="web">Web</option>
			</select>
		</div>
		<div>
			<label for="originalFilename">Original Filename:</label>
			<input type="text" id="originalFilename" bind:value={originalFilename} required />
		</div>
		<div>
			<label for="documentType">Document Type:</label>
			<select id="documentType" bind:value={documentType}>
				<option value="INVOICE">Invoice</option>
				<option value="STATEMENT">Statement</option>
			</select>
		</div>
		<button type="submit">Upload</button>
	</form>

	{#if error}
		<p style="color: red;">{error}</p>
	{/if}

	{#if response}
		<div>
			<h2>Upload Successful</h2>
			<p><strong>Document ID:</strong> {response.documentId}</p>
			<p><strong>Status:</strong> {response.status}</p>
			<p><strong>Created:</strong> {response.created}</p>
			<p><strong>Original Filename:</strong> {response.originalFilename}</p>
		</div>
	{/if}
</section>

<section>
	<h1>Get Document Info</h1>
	<div>
		<label for="documentId">Document ID:</label>
		<input type="text" id="documentId" bind:value={documentId} placeholder="Enter document ID from upload" />
		<button on:click={getDocumentInfo}>Get Info</button>
		<button on:click={previewDocument} style="margin-left: 10px;">Preview File</button>
	</div>

	{#if getError}
		<p style="color: red;">{getError}</p>
	{/if}

	{#if documentInfo}
		<div>
			<h2>Document Information</h2>
			<p><strong>Document ID:</strong> {documentInfo.documentId}</p>
			<p><strong>Status:</strong> {documentInfo.status}</p>
			<p><strong>Document Type:</strong> {documentInfo.documentType}</p>
			<p><strong>Created:</strong> {documentInfo.created}</p>
			<p><strong>Original Filename:</strong> {documentInfo.originalFilename}</p>
		</div>
	{/if}
</section>

<section>
	<h1>Create Job</h1>
	<div>
		<label for="jobDocumentId">Document ID:</label>
		<input type="text" id="jobDocumentId" bind:value={documentId} placeholder="Enter document ID from upload" />
	</div>
	<div>
		<label for="pipeline">Pipeline:</label>
		<select id="pipeline" bind:value={pipeline}>
			<option value="DEFAULT">DEFAULT</option>
			<option value="FAST">FAST</option>
			<option value="ACCURATE">ACCURATE</option>
		</select>
	</div>
	<div>
		<label>
			<input type="checkbox" bind:checked={useOcr} /> Use OCR
		</label>
	</div>
	<div>
		<label>
			<input type="checkbox" bind:checked={useAi} /> Use AI
		</label>
	</div>
	<div>
		<label for="languageHint">Language Hint:</label>
		<input type="text" id="languageHint" bind:value={languageHint} placeholder="e.g., nb, en, sv" />
	</div>
	<button on:click={createJob}>Create Job</button>

	{#if jobError}
		<p style="color: red;">{jobError}</p>
	{/if}

	{#if jobResponse}
		<div>
			<h2>Job Created Successfully</h2>
			<p><strong>Job ID:</strong> {jobResponse.jobId}</p>
			<p><strong>Document ID:</strong> {jobResponse.documentId}</p>
			<p><strong>Status:</strong> {jobResponse.status}</p>
			<p><strong>Created:</strong> {jobResponse.created}</p>
		</div>
	{/if}
</section>

<section>
	<h1>Get Job Status</h1>
	<div>
		<label for="jobId">Job ID:</label>
		<input type="text" id="jobId" bind:value={jobId} placeholder="Enter job ID from job creation" />
		<button on:click={getJobStatus}>Get Status</button>
	</div>

	{#if jobStatusError}
		<p style="color: red;">{jobStatusError}</p>
	{/if}

	{#if jobStatus}
		<div>
			<h2>Job Status</h2>
			<p><strong>Job ID:</strong> {jobStatus.jobId}</p>
			<p><strong>Document ID:</strong> {jobStatus.documentId}</p>
			<p><strong>Status:</strong> {jobStatus.status}</p>
			<p><strong>Started At:</strong> {jobStatus.startedAt || 'Not started'}</p>
			<p><strong>Finished At:</strong> {jobStatus.finishedAt || 'Not finished'}</p>
			<p><strong>Error:</strong> {jobStatus.error || 'None'}</p>
		</div>
	{/if}
</section>

<section>
	<h1>Cancel Job</h1>
	<div>
		<label for="cancelJobId">Job ID:</label>
		<input type="text" id="cancelJobId" bind:value={jobId} placeholder="Enter job ID to cancel" />
		<button on:click={cancelJob}>Cancel Job</button>
	</div>

	{#if cancelJobError}
		<p style="color: red;">{cancelJobError}</p>
	{/if}

	{#if cancelJobResponse}
		<div>
			<h2>Job Cancelled Successfully</h2>
			<p><strong>Job ID:</strong> {cancelJobResponse.jobId}</p>
			<p><strong>Status:</strong> {cancelJobResponse.status}</p>
			<p><strong>Cancelled At:</strong> {cancelJobResponse.cancelledAt}</p>
		</div>
	{/if}
</section>

<section>
	<h1>Get Extraction Result</h1>
	<div>
		<label for="extractionDocumentId">Document ID:</label>
		<input type="text" id="extractionDocumentId" bind:value={documentId} placeholder="Enter document ID from upload" />
		<button on:click={getExtractionResult}>Get Extraction Result</button>
	</div>

	{#if extractionResultError}
		<p style="color: red;">{extractionResultError}</p>
	{/if}

	{#if extractionResult}
		<div>
			<h2>Extraction Result</h2>
			<p><strong>Document ID:</strong> {extractionResult.documentId}</p>
			<p><strong>Document Type:</strong> {extractionResult.documentType}</p>
			<p><strong>Extraction Version:</strong> {extractionResult.extractionVersion}</p>
			
			{#if extractionResult.documentType === 'STATEMENT'}
				{#if extractionResult.transactions && extractionResult.transactions.length > 0}
					<h3>Transactions</h3>
					<table style="border-collapse: collapse; width: 100%;">
						<thead>
							<tr>
								<th style="border: 1px solid #ddd; padding: 8px; text-align: left;">Date</th>
								<th style="border: 1px solid #ddd; padding: 8px; text-align: left;">Amount</th>
								<th style="border: 1px solid #ddd; padding: 8px; text-align: left;">Currency</th>
								<th style="border: 1px solid #ddd; padding: 8px; text-align: left;">Description</th>
								<th style="border: 1px solid #ddd; padding: 8px; text-align: left;">Confidence</th>
							</tr>
						</thead>
						<tbody>
							{#each extractionResult.transactions as transaction}
								<tr>
									<td style="border: 1px solid #ddd; padding: 8px;">{transaction.date}</td>
									<td style="border: 1px solid #ddd; padding: 8px;">{transaction.amount}</td>
									<td style="border: 1px solid #ddd; padding: 8px;">{transaction.currency}</td>
									<td style="border: 1px solid #ddd; padding: 8px;">{transaction.description}</td>
									<td style="border: 1px solid #ddd; padding: 8px;">
										{#if transaction.confidence}
											Date: {transaction.confidence.date}, Amount: {transaction.confidence.amount}
										{/if}
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				{/if}
			{:else if extractionResult.documentType === 'INVOICE'}
				{#if extractionResult.extractedAt}
					<p><strong>Extracted At:</strong> {extractionResult.extractedAt}</p>
				{/if}
				
				{#if extractionResult.fields}
					<h3>Extracted Fields</h3>
					{#each Object.entries(extractionResult.fields) as [key, value]}
						<p><strong>{key.charAt(0).toUpperCase() + key.slice(1)}:</strong> {value}</p>
					{/each}
				{/if}
				
				{#if extractionResult.confidence}
					<h3>Confidence Scores</h3>
					{#each Object.entries(extractionResult.confidence) as [field, score]}
						<p><strong>{field.charAt(0).toUpperCase() + field.slice(1)}:</strong> {score}</p>
					{/each}
				{/if}
				
				{#if extractionResult.warnings && extractionResult.warnings.length > 0}
					<h3>Warnings</h3>
					<ul>
						{#each extractionResult.warnings as warning}
							<li>{warning}</li>
						{/each}
					</ul>
				{/if}
			{/if}
		</div>
	{/if}
</section>

<section>
	<h1>Save Correction</h1>
	<div>
		<label for="correctionDocumentId">Document ID:</label>
		<input type="text" id="correctionDocumentId" bind:value={documentId} placeholder="Enter document ID" />
	</div>
	<div>
		<label for="correctionDocumentType">Document Type:</label>
		<select id="correctionDocumentType" bind:value={correctionDocumentType}>
			<option value="INVOICE">Invoice</option>
			<option value="STATEMENT">Statement</option>
		</select>
	</div>
	<div>
		<label for="correctionFields">Fields (JSON):</label>
		<textarea id="correctionFields" bind:value={correctionFields} rows="6" placeholder='{"date": "2026-01-02", "amount": 12450.00}'></textarea>
	</div>
	<div>
		<label for="correctionNote">Note:</label>
		<input type="text" id="correctionNote" bind:value={correctionNote} placeholder="Enter correction note" />
	</div>
	<button on:click={saveCorrection}>Save Correction</button>

	{#if correctionError}
		<p style="color: red;">{correctionError}</p>
	{/if}

	{#if correctionResponse}
		<div>
			<h2>Correction Saved</h2>
			<p><strong>Document ID:</strong> {correctionResponse.documentId}</p>
			<p><strong>Correction Version:</strong> {correctionResponse.correctionVersion}</p>
			<p><strong>Saved At:</strong> {correctionResponse.savedAt}</p>
			<p><strong>Saved By:</strong> {correctionResponse.savedBy}</p>
			<p><strong>Normalized Transactions Created:</strong> {correctionResponse.normalizedTransactionsCreated}</p>
		</div>
	{/if}
</section>

<style>
	section {
		max-width: 600px;
		margin: 0 auto;
		padding: 20px;
	}

	form div {
		margin-bottom: 10px;
	}

	label {
		display: block;
		margin-bottom: 5px;
	}

	input, select {
		width: 100%;
		padding: 8px;
		box-sizing: border-box;
	}

	textarea {
		width: 100%;
		padding: 8px;
		box-sizing: border-box;
		font-family: monospace;
	}

	button {
		padding: 10px 20px;
		background-color: #007bff;
		color: white;
		border: none;
		cursor: pointer;
	}

	button:hover {
		background-color: #0056b3;
	}
</style>
