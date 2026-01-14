<script>
	import { onMount } from 'svelte';

	let documents = [];
	let loading = true;
	let error = null;
	let selectedDocument = null;
	let interpretationStarted = false;

	// Interpretation settings
	let useOcr = false;
	let useAi = true;
	let languageHint = 'nb';
	let hintedType = 'INVOICE';
	let jobResponse = null;
	let jobError = null;

	onMount(async () => {
		await loadDocuments();
	});

	async function loadDocuments() {
		loading = true;
		error = null;

		try {
			const res = await fetch('/api/v1/documents');
			if (res.ok) {
				documents = await res.json();
				// Sort by created date, newest first
				documents.sort((a, b) => new Date(b.uploadedAt) - new Date(a.uploadedAt));
			} else {
				error = `Error loading documents: ${res.status}`;
			}
		} catch (err) {
			error = `Network error: ${err.message}`;
		} finally {
			loading = false;
		}
	}

	function selectDocument(doc) {
		selectedDocument = doc;
		interpretationStarted = true;
		hintedType = doc.documentType;
		jobResponse = null;
		jobError = null;
	}

	function cancelInterpretation() {
		selectedDocument = null;
		interpretationStarted = false;
		jobResponse = null;
		jobError = null;
	}

	async function startInterpretation() {
		if (!selectedDocument) return;

		jobError = null;
		jobResponse = null;

		const request = {
			useOcr: useOcr,
			useAi: useAi,
			languageHint: languageHint,
			hintedType: hintedType
		};

		try {
			const res = await fetch(
				`/api/v1/interpretation/documents/${selectedDocument.id}/jobs`,
				{
					method: 'POST',
					headers: {
						'Content-Type': 'application/json'
					},
					body: JSON.stringify(request)
				}
			);

			if (res.ok) {
				jobResponse = await res.json();
				jobError = null;
			} else if (res.status === 404) {
				jobError = 'Document not found';
			} else {
				const errorText = await res.text();
				jobError = `Error: ${res.status} - ${errorText}`;
			}
		} catch (err) {
			jobError = `Network error: ${err.message}`;
		}
	}
</script>

<svelte:head>
	<title>Tolk - PocketAccount</title>
</svelte:head>

<div class="container">
	<h1>Tolk dokumenter</h1>

	{#if loading}
		<div class="loading">Laster dokumenter...</div>
	{:else if error}
		<div class="alert alert-error">{error}</div>
	{:else if !interpretationStarted}
		<div class="documents-list">
			<p class="info">Klikk på et dokument-ID for å starte tolking</p>
			<table>
				<thead>
					<tr>
						<th>Dokument-ID</th>
						<th>Type</th>
						<th>Original filnavn</th>
						<th>Lastet opp</th>
					</tr>
				</thead>
				<tbody>
					{#each documents as doc}
						<tr>
							<td>
								<button class="doc-id-btn" on:click={() => selectDocument(doc)}>
									{doc.id}
								</button>
							</td>
							<td>{doc.documentType}</td>
							<td>{doc.originalFilename}</td>
							<td>{new Date(doc.uploadedAt).toLocaleString('nb-NO')}</td>
						</tr>
					{:else}
						<tr>
							<td colspan="4" class="no-data">Ingen dokumenter funnet</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{:else}
		<div class="interpretation-panel">
			<div class="panel-header">
				<h2>Tolk dokument: {selectedDocument.originalFilename}</h2>
				<button on:click={cancelInterpretation} class="btn-secondary">Avbryt</button>
			</div>

			<div class="document-info">
				<p><strong>ID:</strong> {selectedDocument.id}</p>
				<p><strong>Type:</strong> {selectedDocument.documentType}</p>
				<p><strong>Lastet opp:</strong> {new Date(selectedDocument.uploadedAt).toLocaleString('nb-NO')}</p>
			</div>

			<div class="interpretation-settings">
				<h3>Tolkingsinnstillinger</h3>

				<div class="form-group">
					<label>
						<input type="checkbox" bind:checked={useOcr} />
						Bruk OCR (for skannede dokumenter)
					</label>
				</div>

				<div class="form-group">
					<label>
						<input type="checkbox" bind:checked={useAi} />
						Bruk AI-tolking
					</label>
				</div>

				<div class="form-group">
					<label for="languageHint">Språk:</label>
					<select id="languageHint" bind:value={languageHint}>
						<option value="nb">Norsk (Bokmål)</option>
						<option value="nn">Norsk (Nynorsk)</option>
						<option value="eng">Engelsk</option>
						<option value="deu">Tysk</option>
						<option value="fra">Fransk</option>
						<option value="eng+deu+fra">Multi-språk</option>
					</select>
				</div>

				<div class="form-group">
					<label for="hintedType">Dokumenttype:</label>
					<select id="hintedType" bind:value={hintedType}>
						<option value="INVOICE">INVOICE</option>
						<option value="STATEMENT">STATEMENT</option>
						<option value="RECEIPT">RECEIPT</option>
						<option value="OTHER">OTHER</option>
					</select>
				</div>

				<button on:click={startInterpretation} class="btn-primary">Start tolking</button>
			</div>

			{#if jobError}
				<div class="alert alert-error">
					<strong>Feil:</strong> {jobError}
				</div>
			{/if}

			{#if jobResponse}
				<div class="alert alert-success">
					<h3>Tolkingsjobb startet!</h3>
					<p><strong>Jobb-ID:</strong> {jobResponse.jobId}</p>
					<p><strong>Dokument-ID:</strong> {jobResponse.documentId}</p>
					<p><strong>Status:</strong> {jobResponse.status}</p>
					<p><strong>Type:</strong> {jobResponse.documentType}</p>
					<p><strong>Opprettet:</strong> {new Date(jobResponse.created).toLocaleString('nb-NO')}</p>
					<p class="info-text">Gå til "Resultater" for å se fremdrift og resultat</p>
				</div>
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

	.alert-success {
		background: #efe;
		border: 1px solid #cfc;
		color: #363;
	}

	.alert h3 {
		margin-top: 0;
	}

	.info {
		background: #e3f2fd;
		padding: 12px;
		border-radius: 4px;
		margin-bottom: 20px;
		color: #1976d2;
	}

	.info-text {
		margin-top: 15px;
		padding-top: 15px;
		border-top: 1px solid #cfc;
		font-weight: 600;
	}

	.documents-list {
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
	}

	td {
		padding: 12px;
		border-bottom: 1px solid #dee2e6;
	}

	tr:hover {
		background: #f8f9fa;
	}

	.doc-id-btn {
		background: #3498db;
		color: white;
		border: none;
		padding: 6px 12px;
		border-radius: 4px;
		cursor: pointer;
		font-weight: 600;
		transition: background 0.2s;
	}

	.doc-id-btn:hover {
		background: #2980b9;
	}

	.no-data {
		text-align: center;
		color: #999;
		font-style: italic;
	}

	.interpretation-panel {
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

	.document-info {
		background: #f8f9fa;
		padding: 15px;
		border-radius: 4px;
		margin-bottom: 20px;
	}

	.document-info p {
		margin: 5px 0;
	}

	.interpretation-settings {
		margin-top: 20px;
	}

	.interpretation-settings h3 {
		color: #2c3e50;
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

	.form-group input[type='checkbox'] {
		margin-right: 8px;
	}

	.form-group select {
		width: 100%;
		padding: 10px;
		border: 1px solid #ddd;
		border-radius: 4px;
		font-size: 14px;
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

	.btn-primary:hover {
		background: #2980b9;
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
