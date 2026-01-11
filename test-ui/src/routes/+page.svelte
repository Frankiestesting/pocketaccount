<script>
	let files = [];
	let source = 'mobile';
	let originalFilename = '';
	let response = null;
	let error = null;

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
