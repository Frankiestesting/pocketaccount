<script>
	let files = [];
	let source = 'mobile';
	let originalFilename = '';
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
