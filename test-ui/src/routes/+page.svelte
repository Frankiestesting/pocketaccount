<script>
	let files = [];
	let source = 'mobile';
	let originalFilename = '';
	let response = null;
	let error = null;
	let documentId = '';
	let documentInfo = null;
	let getError = null;

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
