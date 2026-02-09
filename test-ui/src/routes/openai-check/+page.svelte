<script>
	import { onMount } from 'svelte';

	let status = 'idle';
	let statusCode = null;
	let responseBody = '';
	let lastCheckedAt = null;

	async function checkConnection() {
		status = 'checking';
		statusCode = null;
		responseBody = '';

		try {
			const res = await fetch('/api/v1/interpretation/openai/check');
			statusCode = res.status;
			responseBody = await res.text();
			lastCheckedAt = new Date();
			status = res.ok ? 'ok' : 'error';
		} catch (err) {
			status = 'error';
			statusCode = null;
			responseBody = err?.message || 'Network error';
			lastCheckedAt = new Date();
		}
	}

	onMount(() => {
		checkConnection();
	});
</script>

<svelte:head>
	<title>OpenAI Sjekk - PocketAccount</title>
</svelte:head>

<div class="container">
	<h1>OpenAI-tilkobling</h1>

	<div class="panel">
		<div class="status-row">
			<div class="signal {status === 'ok' ? 'signal--ok' : status === 'error' ? 'signal--error' : 'signal--idle'}"></div>
			<div class="status-text">
				{#if status === 'checking'}
					Sjekker tilkobling...
				{:else if status === 'ok'}
					Tilkobling OK
				{:else if status === 'error'}
					Tilkobling FEIL
				{:else}
					Ikke sjekket
				{/if}
			</div>
			<button class="btn" on:click={checkConnection} disabled={status === 'checking'}>
				{status === 'checking' ? 'Sjekker...' : 'Sjekk igjen'}
			</button>
		</div>

		{#if lastCheckedAt}
			<div class="meta">
				Sist sjekket: {lastCheckedAt.toLocaleString('nb-NO')}
				{#if statusCode !== null}
					<span class="code">HTTP {statusCode}</span>
				{/if}
			</div>
		{/if}

		{#if status === 'error'}
			<div class="error-box">
				<div class="error-title">OpenAI-feil</div>
				<pre>{responseBody || 'Ingen detaljer mottatt.'}</pre>
			</div>
		{:else if status === 'ok'}
			<details class="details">
				<summary>Vis svar fra OpenAI</summary>
				<pre>{responseBody || 'Ingen detaljer mottatt.'}</pre>
			</details>
		{/if}
	</div>
</div>

<style>
	.container {
		max-width: 900px;
	}

	h1 {
		color: #2c3e50;
		margin-bottom: 24px;
	}

	.panel {
		background: white;
		padding: 24px;
		border-radius: 8px;
		box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
	}

	.status-row {
		display: flex;
		align-items: center;
		gap: 12px;
		flex-wrap: wrap;
	}

	.signal {
		width: 16px;
		height: 16px;
		border-radius: 50%;
		background: #bdc3c7;
		box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.05) inset;
	}

	.signal--ok {
		background: #2ecc71;
	}

	.signal--error {
		background: #e74c3c;
	}

	.signal--idle {
		background: #f1c40f;
	}

	.status-text {
		font-weight: 600;
		color: #2c3e50;
	}

	.btn {
		margin-left: auto;
		background: #3498db;
		color: white;
		border: none;
		padding: 8px 14px;
		border-radius: 4px;
		cursor: pointer;
		font-weight: 600;
	}

	.btn:disabled {
		opacity: 0.6;
		cursor: not-allowed;
	}

	.meta {
		margin-top: 12px;
		color: #7f8c8d;
		font-size: 14px;
		display: flex;
		gap: 12px;
		align-items: center;
		flex-wrap: wrap;
	}

	.code {
		background: #ecf0f1;
		padding: 2px 8px;
		border-radius: 999px;
		font-weight: 600;
		color: #2c3e50;
	}

	.error-box {
		margin-top: 16px;
		padding: 12px;
		border-radius: 6px;
		background: #fee;
		border: 1px solid #fcc;
		color: #c33;
	}

	.error-title {
		font-weight: 700;
		margin-bottom: 8px;
	}

	.details {
		margin-top: 16px;
	}

	pre {
		margin: 0;
		white-space: pre-wrap;
		word-break: break-word;
	}
</style>
