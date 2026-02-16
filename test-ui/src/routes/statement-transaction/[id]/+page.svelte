<script>
	import { onMount } from 'svelte';
	import { page } from '$app/stores';

	let transaction = null;
	let loading = true;
	let error = null;
	let currentId = null;

	$: statementTransactionId = $page.params.id;

	$: if (statementTransactionId && statementTransactionId !== currentId) {
		currentId = statementTransactionId;
		loadTransaction();
	}

	onMount(async () => {
		if (statementTransactionId) {
			await loadTransaction();
		}
	});

	async function loadTransaction() {
		loading = true;
		error = null;
		try {
			const res = await fetch(`/api/v1/interpretation/statement-transactions/${statementTransactionId}`);
			if (!res.ok) {
				throw new Error(`Failed to fetch statement transaction: ${res.status}`);
			}
			transaction = await res.json();
		} catch (err) {
			error = err instanceof Error ? err.message : String(err);
		} finally {
			loading = false;
		}
	}

	function formatDate(dateString) {
		if (!dateString) return '-';
		const date = new Date(dateString);
		if (Number.isNaN(date.getTime())) return dateString;
		return date.toLocaleDateString('nb-NO');
	}

	function formatAmount(amount) {
		if (amount === null || amount === undefined) return '-';
		return Number(amount).toLocaleString('nb-NO', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
	}
</script>

<svelte:head>
	<title>Statement transaction</title>
</svelte:head>

<div class="container">
	<h1>Statement transaction</h1>

	{#if loading}
		<div class="loading">Laster...</div>
	{:else if error}
		<div class="alert error">{error}</div>
	{:else if !transaction}
		<div class="alert info">Ingen data funnet.</div>
	{:else}
		<div class="panel">
			<p><strong>ID:</strong> {transaction.id}</p>
			<p><strong>Dato:</strong> {formatDate(transaction.date)}</p>
			<p><strong>Beskrivelse:</strong> {transaction.description || '-'}</p>
			<p><strong>Valuta:</strong> {transaction.currency || '-'}</p>
			<p><strong>Belop:</strong> {formatAmount(transaction.amount)}</p>
			<p><strong>Konto:</strong> {transaction.accountNo || '-'}</p>
			<p><strong>Status:</strong> {transaction.approved ? 'Approved' : 'Pending'}</p>
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
