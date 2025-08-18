package com.eomaxl.bankapplication.mapper;

import com.eomaxl.bankapplication.domain.model.*;
import com.eomaxl.bankapplication.dto.*;
import com.eomaxl.bankapplication.dto.response.TransferResponse;
import com.eomaxl.bankapplication.service.impl.BankingFacadeServiceImpl;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BankingMapper {

    BankingMapper INSTANCE = Mappers.getMapper(BankingMapper.class);

    // Person mappings
    PersonDto toPersonDto(Person person);
    Person toPerson(PersonDto personDto);
    List<PersonDto> toPersonDtos(List<Person> persons);

    // Bank mappings
    BankDto toBankDto(Bank bank);
    Bank toBank(BankDto bankDto);
    List<BankDto> toBankDtos(List<Bank> banks);

    // AccountHolder mappings
    AccountHolderDto toAccountHolderDto(AccountHolder accountHolder);
    AccountHolder toAccountHolder(AccountHolderDto accountHolderDto);
    List<AccountHolderDto> toAccountHolderDtos(List<AccountHolder> accountHolders);

    // Account mappings
    @Mapping(source = "bank", target = "bank")
    @Mapping(source = "accountHolder", target = "accountHolder")
    AccountDto toAccountDto(Account account);

    @Mapping(source = "bank", target = "bank")
    @Mapping(source = "accountHolder", target = "accountHolder")
    Account toAccount(AccountDto accountDto);

    List<AccountDto> toAccountDtos(List<Account> accounts);

    // Transaction mappings
    @Mapping(source = "account.accountNumber", target = "accountNumber")
    @Mapping(source = "targetAccount.accountNumber", target = "targetAccountNumber")
    TransactionDto toTransactionDto(Transaction transaction);

    @Mapping(source = "accountNumber", target = "account.accountNumber")
    @Mapping(source = "targetAccountNumber", target = "targetAccount.accountNumber")
    Transaction toTransaction(TransactionDto transactionDto);

    List<TransactionDto> toTransactionDtos(List<Transaction> transactions);

    // Transfer response mapping
    @Mapping(source = "transactions", target = "transactions")
    TransferResponse toTransferResponse(BankingFacadeServiceImpl.TransferResult transferResult);
}