<script>
	/** @type {FileList | undefined} */
	let files;
	let originalFilename = '';
	let documentType = 'INVOICE';
	let source = 'web';
	let uploading = false;
	/** @type {{ documentId: string, originalFilename: string, documentType: string, fileSize: number, created: string } | null} */
	let response = null;
	/** @type {string | null} */
	let error = null;

	$: file = files?.[0];

	/** @param {unknown} err */
	function getErrorMessage(err) {
		return err instanceof Error ? err.message : String(err);
	}

	async function handleUpload() {
		if (!file) {
			error = 'Please select a file';
			return;
		}
		if (!originalFilename) {
			error = 'Please enter original filename';
			return;
		}

		uploading = true;
		error = null;
		response = null;

		const formData = new FormData();
		formData.append('file', file);
		formData.append('source', source);
		formData.append('originalFilename', originalFilename);
		formData.append('documentType', documentType);

		try {
			const res = await fetch('/api/v1/documents', {
				method: 'POST',
				body: formData
			});

			if (res.ok) {
				response = await res.json();
				error = null;
				// Reset form
				files = undefined;
				originalFilename = '';
			} else {
				const errorData = await res.text();
				error = `Error: ${res.status} - ${errorData}`;
				response = null;
			}
		} catch (err) {
			error = `Network error: ${getErrorMessage(err)}`;
			response = null;
		} finally {
			uploading = false;
		}
	}
</script>

<svelte:head>
	<title>Last opp - PocketAccount</title>
</svelte:head>

<div class="container">
	<h1>Last opp dokument</h1>

	<div class="upload-form">
		<div class="form-group">
			<label for="file">Velg PDF-fil:</label>
			<input type="file" id="file" accept=".pdf" bind:files disabled={uploading} />
			{#if file}
				<p class="file-info">Valgt: {file.name} ({(file.size / 1024).toFixed(2)} KB)</p>
			{/if}
		</div>

		<div class="form-group">
			<label for="originalFilename">Original filnavn:</label>
			<input
				type="text"
				id="originalFilename"
				bind:value={originalFilename}
				placeholder="f.eks. faktura_2026_01.pdf"
				disabled={uploading}
			/>
		</div>

		<div class="form-group">
			<label for="documentType">Dokumenttype:</label>
			<select id="documentType" bind:value={documentType} disabled={uploading}>
				<option value="INVOICE">INVOICE</option>
				<option value="STATEMENT">STATEMENT</option>
				<option value="RECEIPT">RECEIPT</option>
				<option value="OTHER">OTHER</option>
			</select>
		</div>

		<div class="form-group">
			<label for="source">Kilde:</label>
			<select id="source" bind:value={source} disabled={uploading}>
				<option value="web">Web</option>
				<option value="mobile">Mobile</option>
				<option value="email">Email</option>
			</select>
		</div>

		<button on:click={handleUpload} disabled={uploading || !file || !originalFilename} class="btn-primary">
			{uploading ? 'Laster opp...' : 'Last opp'}
		</button>
	</div>

	{#if error}
		<div class="alert alert-error">
			<strong>Feil:</strong> {error}
		</div>
	{/if}

	{#if response}
		<div class="alert alert-success">
			<h2>Dokument lastet opp!</h2>
			<p><strong>ID:</strong> {response.documentId}</p>
			<p><strong>Filnavn:</strong> {response.originalFilename}</p>
			<p><strong>Type:</strong> {response.documentType}</p>
			<p><strong>Størrelse:</strong> {(response.fileSize / 1024).toFixed(2)} KB</p>
			<p><strong>Lastet opp:</strong> {new Date(response.created).toLocaleString('nb-NO')}</p>
		</div>
	{/if}
</div>

<style>
	.container {
		max-width: 800px;
	}

	h1 {
		color: #2c3e50;
		margin-bottom: 30px;
	}

	.upload-form {
		background: white;
		padding: 30px;
		border-radius: 8px;
		box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
		margin-bottom: 20px;
	}

	.form-group {
		margin-bottom: 20px;
	}

	.form-group label {
		display: block;
		margin-bottom: 8px;
		font-weight: 600;
		color: #2c3e50;
	}

	.form-group input[type='text'],
	.form-group select {
		width: 100%;
		padding: 10px;
		border: 1px solid #ddd;
		border-radius: 4px;
		font-size: 14px;
		box-sizing: border-box;
	}

	.form-group input[type='file'] {
		width: 100%;
		padding: 10px;
		border: 2px dashed #ddd;
		border-radius: 4px;
		background: #f8f9fa;
	}

	.file-info {
		margin-top: 8px;
		font-size: 14px;
		color: #666;
	}

	.btn-primary {
		background: #3498db;
		color: white;
		border: none;
		padding: 12px 24px;
		border-radius: 4px;
		font-size: 16px;
		font-weight: 600;
		cursor: pointer;
		transition: background 0.2s;
	}

	.btn-primary:hover:not(:disabled) {
		background: #2980b9;
	}

	.btn-primary:disabled {
		background: #95a5a6;
		cursor: not-allowed;
	}

	.alert {
		padding: 20px;
		border-radius: 8px;
		margin-top: 20px;
	}

	.alert-error {
		background: #fee;
		border: 1px solid #fcc;
		color: #c33;
	}

	.alert-success {
		background: #efe;
		border: 1px solid #cfc;
		color: #363;
	}

	.alert h2 {
		margin-top: 0;
		font-size: 20px;
	}

	.alert p {
		margin: 8px 0;
	}
</style>
