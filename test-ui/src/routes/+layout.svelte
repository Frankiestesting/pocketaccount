<script>
	import { page } from '$app/stores';
	import './styles.css';

	/** @param {MouseEvent} e @param {string} path */
	function handleClick(e, path) {
		console.log('Link clicked:', path, e);
	}
</script>

<div class="app">
	<nav class="sidebar">
		<h1>PocketAccount</h1>
		<ul>
			<li class:active={$page.url.pathname === '/upload'}>
				<a href="/upload" on:click={(e) => handleClick(e, '/upload')}>Last opp</a>
			</li>
			<li class:active={$page.url.pathname === '/interpret'}>
				<a href="/interpret" on:click={(e) => handleClick(e, '/interpret')}>Tolk</a>
			</li>
			<li class:active={$page.url.pathname === '/results'}>
				<a href="/results" on:click={(e) => handleClick(e, '/results')}>Resultater</a>
			</li>
			<li class:active={$page.url.pathname === '/corrections'}>
				<a href="/corrections" on:click={(e) => handleClick(e, '/corrections')}>Korrigering</a>
			</li>
			<li class:active={$page.url.pathname === '/statement-approve'}>
				<a href="/statement-approve" on:click={(e) => handleClick(e, '/statement-approve')}>Godkjenn</a>
			</li>
			<li class:active={$page.url.pathname === '/receipt-approve'}>
				<a href="/receipt-approve" on:click={(e) => handleClick(e, '/receipt-approve')}>Godkjenn kvittering</a>
			</li>
			<li class:active={$page.url.pathname === '/accounts'}>
				<a href="/accounts" on:click={(e) => handleClick(e, '/accounts')}>Kontoer</a>
			</li>
			<li class:active={$page.url.pathname === '/compare'}>
				<a href="/compare" on:click={(e) => handleClick(e, '/compare')}>Sammenlign</a>
			</li>
			<li class:active={$page.url.pathname === '/compare-results'}>
				<a href="/compare-results" on:click={(e) => handleClick(e, '/compare-results')}>Sammenlign Resultater</a>
			</li>
			<li class:active={$page.url.pathname === '/bank-transactions'}>
				<a href="/bank-transactions" on:click={(e) => handleClick(e, '/bank-transactions')}>Banktransaksjoner</a>
			</li>
			<li class:active={$page.url.pathname === '/openai-check'}>
				<a href="/openai-check" on:click={(e) => handleClick(e, '/openai-check')}>OpenAI Sjekk</a>
			</li>
		</ul>
	</nav>
	<main class="content">
		<slot />
	</main>
</div>

<style>
	.app {
		display: flex;
		height: 100vh;
	}

	.sidebar {
		width: 250px;
		background: #2c3e50;
		color: white;
		padding: 20px;
		box-shadow: 2px 0 5px rgba(0, 0, 0, 0.1);
	}

	.sidebar h1 {
		font-size: 24px;
		margin: 0 0 30px 0;
		padding-bottom: 20px;
		border-bottom: 1px solid rgba(255, 255, 255, 0.2);
		position: relative;
		z-index: 1;
	}

	.sidebar ul {
		list-style: none;
		padding: 0;
		margin: 0;
		position: relative;
		z-index: 2;
	}

	.sidebar li {
		margin-bottom: 10px;
		position: relative;
		z-index: 2;
	}

	.sidebar a {
		display: block;
		padding: 12px 16px;
		color: white;
		text-decoration: none;
		border-radius: 4px;
		transition: background 0.2s;
		position: relative;
		z-index: 3;
		pointer-events: auto;
	}

	.sidebar a:hover {
		background: rgba(255, 255, 255, 0.1);
	}

	.sidebar li.active a {
		background: #3498db;
	}

	.content {
		flex: 1;
		padding: 30px;
		overflow-y: auto;
		background: #ecf0f1;
	}
</style>
