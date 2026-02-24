<script>
	import { onMount } from 'svelte';

	/**
	 * @typedef {Object} Account
	 * @property {string} id
	 * @property {string} name
	 * @property {string} currency
	 * @property {string} accountNo
	 * @property {string} createdAt
	 */
	/**
	 * @typedef {Object} Transaction
	 * @property {string} id
	 * @property {string} [bookingDate]
	 * @property {string} [description]
	 * @property {number} [amount]
	 * @property {string} [currency]
	 */

	/** @type {Account[]} */
	let accounts = [];
	let loading = true;
	/** @type {string|null} */
	let error = null;
	let newAccountName = '';
	let newAccountCurrency = 'NOK';
	let newAccountNo = '';
	let showCreateForm = false;

	/** @type {Account|null} */
	let selectedAccount = null;
	/** @type {Transaction[]} */
	let transactions = [];
	let transactionsLoading = false;
	/** @type {string|null} */
	let transactionsError = null;

	/** @param {unknown} err */
	function getErrorMessage(err) {
		return err instanceof Error ? err.message : String(err);
	}

	async function fetchAccounts() {
		try {
			loading = true;
			error = null;
			const response = await fetch('/api/v1/accounts');
			if (!response.ok) {
				throw new Error(`Failed to fetch accounts: ${response.status}`);
			}
			accounts = await response.json();
		} catch (err) {
			error = getErrorMessage(err);
			console.error('Error fetching accounts:', err);
		} finally {
			loading = false;
		}
	}

	/** @param {string} accountId */
	async function fetchTransactions(accountId) {
		try {
			transactionsLoading = true;
			transactionsError = null;
			transactions = [];

			const response = await fetch(`/api/v1/accounts/${accountId}/transactions`);
			if (!response.ok) {
				throw new Error(`Failed to fetch transactions: ${response.status}`);
			}
			transactions = await response.json();
		} catch (err) {
			transactionsError = getErrorMessage(err);
			console.error('Error fetching transactions:', err);
		} finally {
			transactionsLoading = false;
		}
	}

	/** @param {Account} account */
	function toggleAccountDetails(account) {
		if (selectedAccount?.id === account.id) {
			selectedAccount = null;
			transactions = [];
			transactionsError = null;
			transactionsLoading = false;
			return;
		}

		selectedAccount = account;
		fetchTransactions(account.id);
	}

	/** @param {Account} account */
	function handleAccountKeydown(/** @type {KeyboardEvent} */ event, account) {
		if (event.key === 'Enter') {
			toggleAccountDetails(account);
		}
	}

	async function createAccount() {
		if (!newAccountName.trim()) {
			alert('Vennligst skriv inn et kontonavn');
			return;
		}

		if (!newAccountCurrency.trim()) {
			alert('Vennligst skriv inn en valuta (f.eks. NOK, EUR, USD)');
			return;
		}

		const accountNoValue = newAccountNo.trim();
		if (!accountNoValue) {
			alert('Vennligst skriv inn kontonummer');
			return;
		}

		if (!/^\d{11}$/.test(accountNoValue)) {
			alert('Kontonummer ma vaere 11 sifre');
			return;
		}

		if (!isValidNorwegianAccountNo(accountNoValue)) {
			alert('Kontonummer er ugyldig (modulus 11)');
			return;
		}

		try {
			const response = await fetch('/api/v1/accounts', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify({
					name: newAccountName,
					currency: newAccountCurrency.trim().toUpperCase(),
					accountNo: accountNoValue
				})
			});

			if (!response.ok) {
				throw new Error(`Failed to create account: ${response.status}`);
			}

			// Reset form
			newAccountName = '';
			newAccountCurrency = 'NOK';
			newAccountNo = '';
			showCreateForm = false;

			// Refresh accounts list
			await fetchAccounts();
		} catch (err) {
			error = getErrorMessage(err);
			console.error('Error creating account:', err);
		}
	}

	/** @param {string} id */
	async function deleteAccount(id) {
		if (!confirm('Er du sikker på at du vil slette denne kontoen?')) {
			return;
		}

		try {
			const response = await fetch(`/api/v1/accounts/${id}`, {
				method: 'DELETE'
			});

			if (!response.ok) {
				throw new Error(`Failed to delete account: ${response.status}`);
			}

			// Refresh accounts list
			await fetchAccounts();

			if (selectedAccount?.id === id) {
				selectedAccount = null;
				transactions = [];
				transactionsError = null;
				transactionsLoading = false;
			}
		} catch (err) {
			error = getErrorMessage(err);
			console.error('Error deleting account:', err);
		}
	}

	onMount(() => {
		fetchAccounts();
	});

	/** @param {string} value */
	function isValidNorwegianAccountNo(value) {
		if (!/^\d{11}$/.test(value)) {
			return false;
		}

		const weights = [2, 3, 4, 5, 6, 7, 2, 3, 4, 5];
		let sum = 0;
		for (let i = 0; i < 10; i += 1) {
			sum += Number(value[9 - i]) * weights[i];
		}
		const remainder = sum % 11;
		let control = 11 - remainder;
		if (control === 11) {
			control = 0;
		}
		if (control === 10) {
			return false;
		}
		return Number(value[10]) === control;
	}
</script>

<div class="accounts-container">
	<div class="header">
		<h1>Kontoer</h1>
		<button class="btn-primary" on:click={() => (showCreateForm = !showCreateForm)}>
			{showCreateForm ? 'Avbryt' : '+ Ny konto'}
		</button>
	</div>

	{#if error}
		<div class="error-message">
			<strong>Feil:</strong> {error}
		</div>
	{/if}

	{#if showCreateForm}
		<div class="create-form">
			<h2>Opprett ny konto</h2>
			<form on:submit|preventDefault={createAccount}>
				<div class="form-group">
					<label for="accountName">Kontonavn:</label>
					<input
						id="accountName"
						type="text"
						bind:value={newAccountName}
						placeholder="Skriv inn kontonavn"
						required
					/>
				</div>
				<div class="form-group">
					<label for="accountCurrency">Valuta:</label>
					<input
						id="accountCurrency"
						type="text"
						bind:value={newAccountCurrency}
						placeholder="NOK"
						maxlength="3"
						required
					/>
				</div>
				<div class="form-group">
					<label for="accountNo">Kontonummer:</label>
					<input
						id="accountNo"
						type="text"
						bind:value={newAccountNo}
						placeholder="11 siffer"
						maxlength="11"
						required
					/>
				</div>
				<div class="form-actions">
					<button type="submit" class="btn-primary">Opprett</button>
					<button type="button" class="btn-secondary" on:click={() => (showCreateForm = false)}>
						Avbryt
					</button>
				</div>
			</form>
		</div>
	{/if}

	{#if loading}
		<div class="loading">Laster kontoer...</div>
	{:else if accounts.length === 0}
		<div class="empty-state">
			<p>Ingen kontoer funnet.</p>
			<p>Opprett en ny konto for å komme i gang.</p>
		</div>
	{:else}
		<div class="accounts-list">
			<table>
				<thead>
					<tr>
						<th>Navn</th>
						<th>Kontonr</th>
						<th>Valuta</th>
						<th>Opprettet</th>
						<th>Handlinger</th>
					</tr>
				</thead>
				<tbody>
					{#each accounts as account}
						<tr
							class:row-selected={selectedAccount?.id === account.id}
							on:click={() => toggleAccountDetails(account)}
							role="button"
							tabindex="0"
							on:keydown={(event) => handleAccountKeydown(event, account)}
						>
							<td class="account-name">{account.name}</td>
							<td>{account.accountNo || '-'}</td>
							<td>{account.currency}</td>
							<td>{new Date(account.createdAt).toLocaleDateString('no-NO')}</td>
							<td>
								<button
									class="btn-danger btn-small"
									on:click|stopPropagation={() => deleteAccount(account.id)}
								>
									Slett
								</button>
							</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>

		{#if selectedAccount}
			<div class="account-details">
				<h2>Innhold: {selectedAccount.name}</h2>

				{#if transactionsError}
					<div class="error-message">
						<strong>Feil:</strong> {transactionsError}
					</div>
				{/if}

				{#if transactionsLoading}
					<div class="loading">Laster transaksjoner...</div>
				{:else if transactions.length === 0}
					<div class="empty-state">
						<p>Ingen transaksjoner funnet.</p>
					</div>
				{:else}
					<div class="transaction-lines">
						{#each transactions as tx}
							<div class="transaction-line">
								<span class="tx-date">{tx.bookingDate ?? '-'}</span>
								<span class="tx-desc">{tx.description ?? '-'}</span>
								<span class="tx-amount">{tx.amount ?? '-'} {tx.currency ?? ''}</span>
							</div>
						{/each}
					</div>
				{/if}
			</div>
		{/if}
	{/if}
</div>

<style>
	.accounts-container {
		padding: 20px;
		max-width: 1200px;
		margin: 0 auto;
	}

	.header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 30px;
	}

	h1 {
		margin: 0;
		color: #2c3e50;
	}

	.error-message {
		background: #fee;
		border: 1px solid #fcc;
		color: #c33;
		padding: 12px;
		border-radius: 4px;
		margin-bottom: 20px;
	}

	.loading {
		text-align: center;
		padding: 40px;
		color: #666;
		font-size: 18px;
	}

	.empty-state {
		text-align: center;
		padding: 60px 20px;
		color: #666;
	}

	.empty-state p {
		margin: 10px 0;
		font-size: 16px;
	}

	.create-form {
		background: #f8f9fa;
		border: 1px solid #dee2e6;
		border-radius: 8px;
		padding: 20px;
		margin-bottom: 30px;
	}

	.create-form h2 {
		margin-top: 0;
		color: #2c3e50;
		font-size: 20px;
	}

	.form-group {
		margin-bottom: 15px;
	}

	.form-group label {
		display: block;
		margin-bottom: 5px;
		font-weight: 500;
		color: #495057;
	}

	.form-group input {
		width: 100%;
		padding: 10px;
		border: 1px solid #ced4da;
		border-radius: 4px;
		font-size: 14px;
	}

	.form-group input:focus {
		outline: none;
		border-color: #3498db;
		box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.1);
	}

	.form-actions {
		display: flex;
		gap: 10px;
		margin-top: 20px;
	}

	.accounts-list {
		background: white;
		border-radius: 8px;
		box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
		overflow: hidden;
	}

	table tbody tr {
		cursor: pointer;
	}

	.row-selected {
		background: #f8f9fa;
	}

	.account-details {
		margin-top: 20px;
		background: white;
		border-radius: 8px;
		box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
		padding: 16px;
	}

	.account-details h2 {
		margin: 0 0 12px 0;
		color: #2c3e50;
		font-size: 18px;
	}

	.transaction-lines {
		display: flex;
		flex-direction: column;
		gap: 8px;
	}

	.transaction-line {
		display: grid;
		grid-template-columns: 140px 1fr 160px;
		gap: 12px;
		padding: 10px 12px;
		border: 1px solid #dee2e6;
		border-radius: 6px;
	}

	.tx-date {
		color: #495057;
		font-variant-numeric: tabular-nums;
	}

	.tx-desc {
		color: #2c3e50;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.tx-amount {
		text-align: right;
		font-variant-numeric: tabular-nums;
		color: #2c3e50;
	}

	table {
		width: 100%;
		border-collapse: collapse;
	}

	thead {
		background: #f8f9fa;
	}

	th {
		text-align: left;
		padding: 12px 16px;
		font-weight: 600;
		color: #495057;
		border-bottom: 2px solid #dee2e6;
	}

	td {
		padding: 12px 16px;
		border-bottom: 1px solid #dee2e6;
	}

	tbody tr:hover {
		background: #f8f9fa;
	}

	.account-name {
		font-weight: 500;
		color: #2c3e50;
	}

	.btn-primary,
	.btn-secondary,
	.btn-danger {
		padding: 10px 20px;
		border: none;
		border-radius: 4px;
		cursor: pointer;
		font-size: 14px;
		font-weight: 500;
		transition: all 0.2s;
	}

	.btn-primary {
		background: #3498db;
		color: white;
	}

	.btn-primary:hover {
		background: #2980b9;
	}

	.btn-secondary {
		background: #95a5a6;
		color: white;
	}

	.btn-secondary:hover {
		background: #7f8c8d;
	}

	.btn-danger {
		background: #e74c3c;
		color: white;
	}

	.btn-danger:hover {
		background: #c0392b;
	}

	.btn-small {
		padding: 6px 12px;
		font-size: 12px;
	}
</style>
