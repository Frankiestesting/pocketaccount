<script>
	import { onMount } from 'svelte';

	/**
	 * @typedef {{
	 *  id: string,
	 *  documentType?: string,
	 *  originalFilename?: string,
	 *  uploadedAt?: string
	 * }} DocumentSummary
	 */
	/**
	 * @typedef {{
	 *  jobId: string,
	 *  documentId: string
	 * }} Job
	 */
	/**
	 * @typedef {{
	 *  extractionVersion?: number,
	 *  extractedAt?: string,
	 *  documentType?: string,
	 *  fields?: Record<string, unknown>,
	 *  correctedFields?: Record<string, unknown>,
	 *  warnings?: string[],
	 *  transactions?: unknown[]
	 * }} ExtractionResult
	 */
	/**
	 * @typedef {{
	 *  correctionVersion?: number | null,
	 *  correctionPlacedAt?: string | null,
	 *  savedAt?: string | null,
	 *  normalizedTransactionsCreated?: number | null
	 * }} CorrectionResponse
	 */

	/** @type {DocumentSummary[]} */
	let documents = [];
	let loading = true;
	/** @type {string | null} */
	let error = null;
	let filter = '';
	/** @type {'open' | 'hidden' | 'all'} */
	let filterMode = 'open';
	/** @type {DocumentSummary | null} */
	let selectedDocument = null;
	/** @type {Set<string>} */
	let hiddenJobIds = new Set();
	/** @type {Set<string>} */
	let hiddenDocumentIds = new Set();

	let documentId = '';
	let documentType = 'INVOICE';
	let note = '';
	let fieldsText = '{\n  \n}';
	/** @type {ExtractionResult | null} */
	let extraction = null;
	/** @type {string | null} */
	let loadError = null;
	/** @type {string | null} */
	let submitError = null;
	/** @type {string | null} */
	let parsingError = null;
	/** @type {CorrectionResponse | null} */
	let submitResponse = null;
	let loadBusy = false;
	let submitBusy = false;
	let copyMessage = '';
	/** @type {string | null} */
	let deleteError = null;
	/** @type {Set<string>} */
	let deletingIds = new Set();

	onMount(async () => {
		loadHiddenJobs();
		await Promise.all([loadDocuments(), loadJobIndex()]);
	});

	/** @param {string | number | Date | null | undefined} value */
	function formatTimestamp(value) {
		if (!value) return '-';
		return new Date(value).toLocaleString('nb-NO');
	}

	/** @param {unknown} err */
	function getErrorMessage(err) {
		return err instanceof Error ? err.message : String(err);
	}

	async function loadDocuments() {
		loading = true;
		error = null;
		deleteError = null;

		try {
			const res = await fetch('/api/v1/documents');
			if (res.ok) {
				documents = await res.json();
				documents.sort(
					(a, b) =>
						new Date(b.uploadedAt || 0).getTime() -
						new Date(a.uploadedAt || 0).getTime()
				);
			} else {
				error = `Error loading documents: ${res.status}`;
			}
		} catch (err) {
			error = `Network error: ${getErrorMessage(err)}`;
		} finally {
			loading = false;
			loadHiddenJobs();
			await loadJobIndex();
		}
	}

	function loadHiddenJobs() {
		try {
			const raw = localStorage.getItem('hiddenJobIds');
			if (!raw) {
				hiddenJobIds = new Set();
				return;
			}
			const ids = JSON.parse(raw);
			hiddenJobIds = new Set(Array.isArray(ids) ? ids : []);
		} catch (err) {
			hiddenJobIds = new Set();
		}
	}

	async function loadJobIndex() {
		if (hiddenJobIds.size === 0) {
			hiddenDocumentIds = new Set();
			return;
		}
		try {
			const res = await fetch('/api/v1/interpretation/jobs');
			if (!res.ok) {
				hiddenDocumentIds = new Set();
				return;
			}
			const jobs = /** @type {Job[]} */ (await res.json());
			const next = new Set();
			jobs.forEach((job) => {
				if (hiddenJobIds.has(job.jobId)) {
					next.add(job.documentId);
				}
			});
			hiddenDocumentIds = next;
		} catch (err) {
			hiddenDocumentIds = new Set();
		}
	}

	/** @param {DocumentSummary} doc */
	async function deleteDocument(doc) {
		if (!doc?.id) {
			return;
		}

		const confirmed = window.confirm(`Delete ${doc.originalFilename || 'this document'}? This cannot be undone.`);
		if (!confirmed) {
			return;
		}

		deleteError = null;
		deletingIds = new Set(deletingIds).add(doc.id);

		try {
			const res = await fetch(`/api/v1/documents/${doc.id}`, {
				method: 'DELETE'
			});
			if (!res.ok) {
				const errorText = await res.text();
				deleteError = `Delete failed: ${res.status} - ${errorText}`;
				return;
			}

			documents = documents.filter((item) => item.id !== doc.id);
			if (selectedDocument?.id === doc.id) {
				selectedDocument = null;
				documentId = '';
				resetEditor();
			}
		} catch (err) {
			deleteError = `Network error: ${getErrorMessage(err)}`;
		} finally {
			const next = new Set(deletingIds);
			next.delete(doc.id);
			deletingIds = next;
		}
	}

	/** @type {DocumentSummary[]} */
	$: filteredDocuments = getVisibleDocuments(
		filter
			? documents.filter((doc) => {
					const name = (doc.originalFilename || '').toLowerCase();
					const id = (doc.id || '').toLowerCase();
					const value = filter.toLowerCase();
					return name.includes(value) || id.includes(value);
				})
			: documents
	);

	/** @param {DocumentSummary} doc */
	function isHiddenDoc(doc) {
		return hiddenDocumentIds.has(doc.id);
	}

	/** @param {DocumentSummary[]} list */
	function getVisibleDocuments(list) {
		if (filterMode === 'all') {
			return list;
		}
		if (filterMode === 'hidden') {
			return list.filter((doc) => isHiddenDoc(doc));
		}
		return list.filter((doc) => !isHiddenDoc(doc));
	}

	/** @param {DocumentSummary} doc */
	function selectDocument(doc) {
		selectedDocument = doc;
		documentId = doc.id;
		documentType = doc.documentType || 'INVOICE';
		loadExtraction();
	}

	async function loadExtraction() {
		if (!documentId) {
			loadError = 'Document ID is required.';
			return;
		}

		loadBusy = true;
		loadError = null;
		extraction = null;
		submitResponse = null;
		submitError = null;
		parsingError = null;

		try {
			const res = await fetch(`/api/v1/documents/${documentId}/result`);
			if (res.ok) {
				const result = await res.json();
				extraction = result;
				if (result.documentType) {
					documentType = result.documentType;
				}
				if (result.documentType === 'STATEMENT' && Array.isArray(result.transactions)) {
					fieldsText = JSON.stringify(
						{ accountNo: result.accountNo || null, transactions: result.transactions },
						null,
						2
					);
				} else {
					const currentFields = result.correctedFields || result.fields || {};
					fieldsText = JSON.stringify(currentFields, null, 2);
				}
			} else if (res.status === 404) {
				loadError = 'Extraction result not found.';
			} else {
				const errorText = await res.text();
				loadError = `Error: ${res.status} - ${errorText}`;
			}
		} catch (err) {
			loadError = `Network error: ${getErrorMessage(err)}`;
		} finally {
			loadBusy = false;
		}
	}

	async function saveCorrection() {
		submitError = null;
		parsingError = null;
		submitResponse = null;

		if (!documentId) {
			submitError = 'Document ID is required.';
			return;
		}

		let fields = {};
		try {
			const trimmed = fieldsText.trim();
			fields = trimmed ? JSON.parse(trimmed) : {};
			if (typeof fields !== 'object' || fields === null) {
				parsingError = 'Fields must be a JSON object.';
				return;
			}
		} catch (err) {
			parsingError = `Invalid JSON: ${getErrorMessage(err)}`;
			return;
		}

		submitBusy = true;

		try {
			const res = await fetch(`/api/v1/documents/${documentId}/correction`, {
				method: 'PUT',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify({
					documentType,
					fields,
					note
				})
			});

			if (res.ok) {
				submitResponse = await res.json();
			} else {
				const errorText = await res.text();
				submitError = `Error: ${res.status} - ${errorText}`;
			}
		} catch (err) {
			submitError = `Network error: ${getErrorMessage(err)}`;
		} finally {
			submitBusy = false;
		}
	}

	function resetEditor() {
		extraction = null;
		fieldsText = '{\n  \n}';
		note = '';
		submitError = null;
		parsingError = null;
		submitResponse = null;
	}

	async function copyProposal() {
		if (!extraction?.fields) {
			copyMessage = 'No proposal to copy.';
			return;
		}

		try {
			await navigator.clipboard.writeText(JSON.stringify(extraction.fields, null, 2));
			copyMessage = 'Proposal copied.';
		} catch (err) {
			copyMessage = 'Copy failed.';
		}

		setTimeout(() => {
			copyMessage = '';
		}, 1500);
	}
</script>

<svelte:head>
	<title>Corrections - PocketAccount</title>
</svelte:head>

<div class="page">
	<div class="hero">
		<div class="hero-text">
			<p class="eyebrow">Document corrections</p>
			<h1>Review and submit corrected fields</h1>
			<p class="subhead">
				Load an extraction result, edit fields as JSON, and save a correction version.
			</p>
		</div>
		<div class="hero-actions">
			<button class="btn ghost" on:click={loadDocuments} disabled={loading}>
				{loading ? 'Loading...' : 'Refresh list'}
			</button>
		</div>
	</div>

	{#if error}
		<div class="alert error">{error}</div>
	{/if}

	<div class="grid">
		<section class="panel list-panel">
			<div class="panel-header">
				<h2>Uploaded documents</h2>
				<span class="badge">{documents.length}</span>
			</div>

			{#if deleteError}
				<div class="alert error">{deleteError}</div>
			{/if}
			<div class="filter">
				<input
					type="text"
					placeholder="Filter by filename or ID"
					bind:value={filter}
				/>
			</div>
			<div class="filter-controls">
				<div class="filter-group" role="radiogroup" aria-label="Filter dokumenter">
					<label class="radio">
						<input
							type="radio"
							name="corrections-filter"
							value="open"
							bind:group={filterMode}
						/>
						<span>Vis åpne</span>
					</label>
					<label class="radio">
						<input
							type="radio"
							name="corrections-filter"
							value="hidden"
							bind:group={filterMode}
						/>
						<span>Vis skjulte</span>
					</label>
					<label class="radio">
						<input
							type="radio"
							name="corrections-filter"
							value="all"
							bind:group={filterMode}
						/>
						<span>Vis alle</span>
					</label>
				</div>
				<span class="filter-note">Skjuling styres fra Resultater.</span>
			</div>
			{#if loading}
				<div class="empty">Loading documents...</div>
			{:else if filteredDocuments.length === 0}
				<div class="empty">No documents match the filter.</div>
			{:else}
				<ul class="doc-list">
					{#each filteredDocuments as doc}
						<li class:selected={doc.id === selectedDocument?.id} class:hidden={filterMode === 'all' && isHiddenDoc(doc)}>
							<div class="doc-row">
								<button class="doc-main" on:click={() => selectDocument(doc)}>
									<span class="doc-name">
										{#if filterMode === 'all' && isHiddenDoc(doc)}
											<span class="hidden-pill">Skjult</span>
										{/if}
										{doc.originalFilename || 'Untitled document'}
									</span>
									<span class="doc-meta">{doc.documentType} • {doc.id}</span>
									<span class="doc-timestamp">Uploaded {formatTimestamp(doc.uploadedAt)}</span>
								</button>
								<button
									class="doc-delete"
									on:click|stopPropagation={() => deleteDocument(doc)}
									disabled={deletingIds.has(doc.id)}
									aria-label="Delete document"
									aria-busy={deletingIds.has(doc.id)}
								>
									<svg viewBox="0 0 24 24" aria-hidden="true">
										<path
											d="M9 3h6l1 2h4v2H4V5h4l1-2zm1 6h2v9h-2V9zm4 0h2v9h-2V9zM7 9h2v9H7V9z"
										/>
									</svg>
								</button>
							</div>
						</li>
					{/each}
				</ul>
			{/if}
		</section>

		<section class="panel editor-panel">
			<div class="panel-header">
				<h2>Correction editor</h2>
				<button class="btn ghost" on:click={resetEditor}>
					Reset
				</button>
			</div>

			<div class="form-grid">
				<label>
					Document ID
					<input type="text" bind:value={documentId} placeholder="UUID" />
				</label>
				<label>
					Document type
					<select bind:value={documentType}>
						<option value="INVOICE">INVOICE</option>
						<option value="STATEMENT">STATEMENT</option>
						<option value="RECEIPT">RECEIPT</option>
						<option value="OTHER">OTHER</option>
					</select>
				</label>
				<div class="actions">
					<button class="btn" on:click={loadExtraction} disabled={loadBusy || !documentId}>
						{loadBusy ? 'Loading...' : 'Load extraction'}
					</button>
				</div>
			</div>

			{#if loadError}
				<div class="alert error">{loadError}</div>
			{/if}

			{#if extraction}
				<div class="extraction-meta">
					<div>
						<span class="meta-label">Version</span>
						<strong>#{extraction.extractionVersion}</strong>
					</div>
					<div>
						<span class="meta-label">Extracted</span>
						<strong>
							{extraction.extractedAt
								? new Date(extraction.extractedAt).toLocaleString('en-GB')
								: '-'}
						</strong>
					</div>
					{#if extraction.warnings?.length}
						<div class="warning-box">
							<strong>Warnings</strong>
							<ul>
								{#each extraction.warnings as warning}
									<li>{warning}</li>
								{/each}
							</ul>
						</div>
					{/if}
				</div>
			{/if}

			<div class="field-grid">
				<label>
					Fields (JSON)
					<textarea rows="14" bind:value={fieldsText}></textarea>
				</label>
				<div class="proposal">
					<div class="proposal-header">
						<span class="proposal-label">Existing extraction (proposal)</span>
						<button class="btn tiny" on:click={copyProposal} disabled={!extraction?.fields}>
							Copy
						</button>
					</div>
					{#if extraction?.fields}
						<pre>{JSON.stringify(extraction.fields, null, 2)}</pre>
					{:else if extraction?.transactions?.length}
						<pre>{JSON.stringify(extraction.transactions, null, 2)}</pre>
					{:else}
						<div class="proposal-empty">Load an extraction result to view the proposal.</div>
					{/if}
					{#if copyMessage}
						<span class="proposal-status">{copyMessage}</span>
					{/if}
				</div>
			</div>

			<label class="full">
				Note
				<input type="text" bind:value={note} placeholder="Add a short note" />
			</label>

			<div class="actions">
				<button class="btn primary" on:click={saveCorrection} disabled={submitBusy}>
					{submitBusy ? 'Saving...' : 'Save correction'}
				</button>
			</div>

			{#if parsingError}
				<div class="alert error">{parsingError}</div>
			{/if}

			{#if submitError}
				<div class="alert error">{submitError}</div>
			{/if}

			{#if submitResponse}
				<div class="alert success">
					<strong>Saved!</strong>
					<span>Version #{submitResponse.correctionVersion}</span>
					<span>
						Correction placed
						{` ${formatTimestamp(
							submitResponse.correctionPlacedAt || submitResponse.savedAt
						)}`}
					</span>
					<span>Normalized transactions: {submitResponse.normalizedTransactionsCreated}</span>
				</div>
			{/if}
		</section>
	</div>
</div>

<style>
	@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;600&family=JetBrains+Mono:wght@400;600&display=swap');

	.page {
		font-family: 'Space Grotesk', sans-serif;
		color: #0f1c2e;
		display: flex;
		flex-direction: column;
		gap: 24px;
	}

	.hero {
		display: flex;
		justify-content: space-between;
		align-items: flex-end;
		padding: 24px 28px;
		border-radius: 18px;
		background: linear-gradient(120deg, #fef3c7, #c7d2fe 55%, #e0f2fe);
		box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
	}

	.eyebrow {
		font-size: 0.85rem;
		letter-spacing: 0.12em;
		text-transform: uppercase;
		margin: 0 0 8px 0;
		color: #334155;
	}

	.hero h1 {
		font-size: 2.2rem;
		margin: 0 0 8px 0;
	}

	.subhead {
		margin: 0;
		color: #475569;
		max-width: 520px;
	}

	.grid {
		display: grid;
		grid-template-columns: 1fr 1.4fr;
		gap: 24px;
	}

	.panel {
		background: #ffffff;
		border-radius: 16px;
		padding: 20px 22px;
		box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
	}

	.panel-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 16px;
	}

	.panel-header h2 {
		margin: 0;
		font-size: 1.2rem;
	}

	.badge {
		background: #0ea5e9;
		color: #fff;
		padding: 4px 10px;
		border-radius: 999px;
		font-size: 0.75rem;
	}

	.filter input {
		width: 100%;
		padding: 10px 12px;
		border-radius: 10px;
		border: 1px solid #e2e8f0;
		background: #f8fafc;
		font-family: inherit;
	}

	.filter-controls {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
		margin-top: 12px;
		padding: 10px 12px;
		border-radius: 12px;
		background: #f1f5f9;
		border: 1px solid #e2e8f0;
	}

	.filter-group {
		display: flex;
		gap: 12px;
		flex-wrap: wrap;
		align-items: center;
	}

	.radio {
		display: inline-flex;
		gap: 6px;
		align-items: center;
		font-size: 0.85rem;
		color: #334155;
		cursor: pointer;
	}

	.filter-note {
		font-size: 0.75rem;
		color: #64748b;
	}

	.doc-list {
		list-style: none;
		padding: 0;
		margin: 16px 0 0 0;
		display: flex;
		flex-direction: column;
		gap: 10px;
	}

	.doc-list li {
		border-radius: 12px;
		border: 1px solid transparent;
		transition: all 0.2s ease;
	}

	.doc-list li.selected {
		border-color: #6366f1;
		background: #eef2ff;
	}

	.doc-list li.hidden {
		background: #f1f5f9;
		border-color: #e2e8f0;
	}

	.doc-row {
		display: flex;
		align-items: center;
		gap: 8px;
	}

	.doc-main {
		width: 100%;
		text-align: left;
		background: transparent;
		border: none;
		padding: 12px 14px;
		cursor: pointer;
		font-family: inherit;
	}

	.doc-delete {
		border: 1px solid transparent;
		background: #fef2f2;
		color: #b91c1c;
		border-radius: 10px;
		padding: 8px;
		cursor: pointer;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
	}

	.doc-delete svg {
		width: 18px;
		height: 18px;
		fill: currentColor;
	}

	.doc-delete:hover:not(:disabled) {
		transform: translateY(-1px);
		box-shadow: 0 6px 14px rgba(185, 28, 28, 0.2);
		border-color: rgba(185, 28, 28, 0.3);
	}

	.doc-delete:disabled {
		opacity: 0.6;
		cursor: not-allowed;
		box-shadow: none;
		transform: none;
	}

	.doc-name {
		display: block;
		font-weight: 600;
		margin-bottom: 4px;
	}

	.hidden-pill {
		background: #e2e8f0;
		color: #475569;
		font-size: 0.7rem;
		padding: 2px 8px;
		border-radius: 999px;
		font-weight: 600;
		margin-right: 6px;
	}

	.doc-meta {
		font-size: 0.78rem;
		color: #64748b;
		word-break: break-all;
	}

	.doc-timestamp {
		display: block;
		margin-top: 4px;
		font-size: 0.75rem;
		color: #94a3b8;
	}

	.empty {
		padding: 16px 0;
		color: #64748b;
		text-align: center;
	}

	.form-grid {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
		gap: 16px;
		margin-bottom: 12px;
	}

	label {
		display: flex;
		flex-direction: column;
		font-size: 0.85rem;
		color: #475569;
		gap: 8px;
	}

	input,
	select,
	textarea {
		font-family: 'JetBrains Mono', monospace;
		font-size: 0.9rem;
		padding: 10px 12px;
		border-radius: 10px;
		border: 1px solid #e2e8f0;
		background: #f8fafc;
		color: #0f172a;
	}

	textarea {
		min-height: 220px;
		resize: vertical;
	}

	.field-grid {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
		gap: 16px;
		margin-top: 16px;
	}

	.proposal {
		display: flex;
		flex-direction: column;
		gap: 8px;
		border: 1px solid #e2e8f0;
		border-radius: 12px;
		padding: 12px;
		background: #f8fafc;
		font-family: 'JetBrains Mono', monospace;
		font-size: 0.85rem;
		color: #0f172a;
	}

	.proposal-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 8px;
	}

	.proposal-label {
		font-size: 0.7rem;
		text-transform: uppercase;
		letter-spacing: 0.12em;
		color: #64748b;
	}

	.proposal pre {
		margin: 0;
		white-space: pre-wrap;
		word-break: break-word;
	}

	.proposal-empty {
		color: #64748b;
		font-size: 0.85rem;
	}

	.proposal-status {
		font-size: 0.75rem;
		color: #475569;
	}

	.full {
		margin-top: 16px;
	}

	.actions {
		display: flex;
		justify-content: flex-start;
		margin-top: 12px;
	}

	.btn {
		border: none;
		border-radius: 999px;
		padding: 10px 18px;
		font-weight: 600;
		cursor: pointer;
		background: #e2e8f0;
		color: #0f172a;
		transition: transform 0.2s ease, box-shadow 0.2s ease;
	}

	.btn:hover:not(:disabled) {
		transform: translateY(-1px);
		box-shadow: 0 8px 18px rgba(15, 23, 42, 0.12);
	}

	.btn.primary {
		background: #0f172a;
		color: #fff;
	}

	.btn.ghost {
		background: transparent;
		border: 1px solid #94a3b8;
		color: #0f172a;
	}

	.btn.tiny {
		padding: 6px 12px;
		font-size: 0.75rem;
	}

	.btn:disabled {
		opacity: 0.6;
		cursor: not-allowed;
		box-shadow: none;
		transform: none;
	}

	.alert {
		margin-top: 16px;
		padding: 12px 14px;
		border-radius: 12px;
		font-size: 0.9rem;
		display: flex;
		flex-direction: column;
		gap: 4px;
	}

	.alert.error {
		background: #fee2e2;
		color: #991b1b;
	}

	.alert.success {
		background: #dcfce7;
		color: #166534;
	}

	.extraction-meta {
		margin: 12px 0 10px 0;
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
		gap: 12px;
	}

	.meta-label {
		display: block;
		font-size: 0.7rem;
		text-transform: uppercase;
		letter-spacing: 0.12em;
		color: #94a3b8;
	}

	.warning-box {
		grid-column: 1 / -1;
		background: #fff7ed;
		color: #9a3412;
		padding: 10px 12px;
		border-radius: 12px;
	}

	.warning-box ul {
		margin: 6px 0 0 16px;
	}

	@media (max-width: 960px) {
		.grid {
			grid-template-columns: 1fr;
		}

		.hero {
			flex-direction: column;
			align-items: flex-start;
			gap: 12px;
		}
	}
</style>
