package com.cts.bfs.etf.corda.schema;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.StatePersistable;

@Entity
@Table(name="ETF_STATE")
public class EtfSchema extends PersistentState implements StatePersistable {
/*
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private long id;
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
*/


    @Column(name = "NAME")
	private String name;
    
    @Column(name = "CODE")
	private String code;

    @Column(name = "QUANTITY")
    private String quantity;
    
    @Column(name = "BUYER")
    private Party buyer;
    
    @Column(name = "SELLER")
    private Party seller;

    @Column(name = "CREATED_DATE")
    private Date createdDate;
	
    @Column(name = "MODIFIED_DATE")
    private Date updatedDate;


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getQuantity() {
		return quantity;
	}

	public void setQuantity(String quantity) {
		this.quantity = quantity;
	}

	public AbstractParty getBuyer() {
		return buyer;
	}

	public void setBuyer(Party buyer) {
		this.buyer = buyer;
	}

	public Party getSeller() {
		return seller;
	}

	public void setSeller(Party seller) {
		this.seller = seller;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public Date getUpdatedDate() {
		return updatedDate;
	}

	public void setUpdatedDate(Date updatedDate) {
		this.updatedDate = updatedDate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((buyer == null) ? 0 : buyer.hashCode());
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		result = prime * result + ((createdDate == null) ? 0 : createdDate.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((quantity == null) ? 0 : quantity.hashCode());
		result = prime * result + ((seller == null) ? 0 : seller.hashCode());
		result = prime * result + ((updatedDate == null) ? 0 : updatedDate.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EtfSchema other = (EtfSchema) obj;
		if (buyer == null) {
			if (other.buyer != null)
				return false;
		} else if (!buyer.equals(other.buyer))
			return false;
		if (code == null) {
			if (other.code != null)
				return false;
		} else if (!code.equals(other.code))
			return false;
		if (createdDate == null) {
			if (other.createdDate != null)
				return false;
		} else if (!createdDate.equals(other.createdDate))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (quantity == null) {
			if (other.quantity != null)
				return false;
		} else if (!quantity.equals(other.quantity))
			return false;
		if (seller == null) {
			if (other.seller != null)
				return false;
		} else if (!seller.equals(other.seller))
			return false;
		if (updatedDate == null) {
			if (other.updatedDate != null)
				return false;
		} else if (!updatedDate.equals(other.updatedDate))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "EtfSchema [name=" + name + ", code=" + code + ", quantity=" + quantity + ", buyer="
				+ buyer + ", seller=" + seller + ", createdDate=" + createdDate + ", updatedDate=" + updatedDate + "]";
	}
    
    
    
}