<script>
	import { onMount } from 'svelte';
	import { page } from '$app/stores';

	/**
	 * @typedef {Object} Receipt
	 * @property {string} id
	 * @property {string} documentId
	 * @property {string} [purchaseDate]
	 * @property {number} [totalAmount]
	 * @property {string} [currency]
	 * @property {string} [merchant]
	 * @property {string} [description]
	 * @property {boolean} [rejected]
	 */

	/** @type {Receipt|null} */
	let receipt = null;
	let loading = true;
	/** @type {string|null} */
	let error = null;
	/** @type {string|null} */
	let currentId = null;

	$: receiptId = $page.params.id;

	$: if (receiptId && receiptId !== currentId) {
		currentId = receiptId;
		loadReceipt();
	}

	onMount(async () => {
		if (receiptId) {
			await loadReceipt();
		}
	});

	/** @param {unknown} err */
	function getErrorMessage(err) {
		return err instanceof Error ? err.message : String(err);
	}

	async function loadReceipt() {
		loading = true;
		error = null;
		try {
			const res = await fetch(`/api/v1/receipts/${receiptId}`);
			if (!res.ok) {
				throw new Error(`Failed to fetch receipt: ${res.status}`);
			}
			receipt = await res.json();
		} catch (err) {
			error = getErrorMessage(err);
		} finally {
			loading = false;
		}
	}

	/** @param {string|undefined} dateString */
	function formatDate(dateString) {
		if (!dateString) return '-';
		const date = new Date(dateString);
		if (Number.isNaN(date.getTime())) return dateString;
		return date.toLocaleDateString('nb-NO');
	}

	/** @param {number|undefined|null} amount */
	function formatAmount(amount) {
		if (amount === null || amount === undefined) return '-';
		return Number(amount).toLocaleString('nb-NO', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
	}
</script>

<svelte:head>
	<title>Kvittering</title>
</svelte:head>

<div class="container">
	<h1>Kvittering</h1>

	{#if loading}
		<div class="loading">Laster...</div>
	{:else if error}
		<div class="alert error">{error}</div>
	{:else if !receipt}
		<div class="alert info">Ingen data funnet.</div>
	{:else}
		<div class="panel">
			<p><strong>ID:</strong> {receipt.id}</p>
			<p><strong>Dokument-ID:</strong> {receipt.documentId}</p>
			<p><strong>Dato:</strong> {formatDate(receipt.purchaseDate)}</p>
			<p><strong>Belop:</strong> {formatAmount(receipt.totalAmount)} {receipt.currency || ''}</p>
			<p><strong>Merchant:</strong> {receipt.merchant || '-'}</p>
			<p><strong>Beskrivelse:</strong> {receipt.description || '-'}</p>
			<p><strong>Status:</strong> {receipt.rejected ? 'Rejected' : 'Active'}</p>
		</div>
	{/if}
</div>

<style>
	.container {
		max-width: 800px;
	}

	h1 {
		color: #2c3e50;
		margin-bottom: 10px;
	}

	.loading {
		text-align: center;
		padding: 32px;
		color: #666;
	}

	.panel {
		background: white;
		padding: 20px;
		border-radius: 8px;
		box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
		margin-top: 16px;
	}

	.alert {
		margin-top: 16px;
		padding: 12px 14px;
		border-radius: 6px;
	}

	.alert.error {
		background: #fee;
		border: 1px solid #fcc;
		color: #c33;
	}

	.alert.info {
		background: #eef6ff;
		border: 1px solid #cfe4ff;
		color: #28527a;
	}
</style>
